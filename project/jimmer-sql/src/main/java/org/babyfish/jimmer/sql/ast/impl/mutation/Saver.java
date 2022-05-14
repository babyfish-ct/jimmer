package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.sql.*;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.impl.query.Queries;
import org.babyfish.jimmer.sql.ast.mutation.AbstractSaveCommand;
import org.babyfish.jimmer.sql.ast.mutation.SimpleSaveResult;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.runtime.Converts;
import org.babyfish.jimmer.sql.runtime.DbNull;
import org.babyfish.jimmer.sql.runtime.ExecutionException;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

import javax.persistence.Id;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

class Saver {

    private AbstractSaveCommandImpl.Data data;

    private Connection con;

    private ImmutableCache cache;

    private Map<String, Integer> affectedRowCountMap;

    Saver(
            AbstractSaveCommandImpl.Data data,
            Connection con
    ) {
        this(data, con, new ImmutableCache(data), new LinkedHashMap<>());
    }

    Saver(
            AbstractSaveCommandImpl.Data data,
            Connection con,
            ImmutableCache cache,
            Map<String, Integer> affectedRowCountMap) {
        this.data = data;
        this.con = con;
        this.cache = cache;
        this.affectedRowCountMap = affectedRowCountMap;
    }

    Saver(Saver base, AbstractSaveCommandImpl.Data data) {
        this.data = data;
        this.con = base.con;
        this.cache = base.cache;
        this.affectedRowCountMap = base.affectedRowCountMap;
    }

    @SuppressWarnings("unchecked")
    public <E> SimpleSaveResult<E> save(E entity) {
        ImmutableType immutableType = ImmutableType.get(entity.getClass());
        E newEntity = (E)Internal.produce(immutableType, entity, draft -> {
            saveImpl((DraftSpi) draft);
        });
        return new SimpleSaveResult<>(affectedRowCountMap, entity, newEntity);
    }

    private void saveImpl(DraftSpi draftSpi) {
        saveAssociations(draftSpi, true);
        saveSelf(draftSpi);
        saveAssociations(draftSpi, false);
    }

    @SuppressWarnings("unchecked")
    private void saveAssociations(DraftSpi currentDraftSpi, boolean forParent) {
        for (ImmutableProp prop : currentDraftSpi.__type().getProps().values()) {
            if (prop.isAssociation() &&
                    prop.getStorage() instanceof Column == forParent &&
                    currentDraftSpi.__isLoaded(prop.getName())
            ) {
                Object associatedValue = currentDraftSpi.__get(prop.getName());
                Set<Object> associatedObjectIds = new LinkedHashSet<>();
                if (associatedValue instanceof List<?>) {
                    List<DraftSpi> associatedObjects = (List<DraftSpi>) associatedValue;
                    for (DraftSpi associatedObject : associatedObjects) {
                        associatedObjectIds.add(saveAssociatedObjectAndGetId(prop, associatedObject));
                    }
                } else {
                    DraftSpi associatedObject = (DraftSpi) associatedValue;
                    associatedObjectIds.add(saveAssociatedObjectAndGetId(prop, associatedObject));
                }
                MiddleTable middleTable = null;
                if (prop.getStorage() instanceof MiddleTable) {
                    middleTable = prop.getStorage();
                } else {
                    ImmutableProp mappedBy = prop.getMappedBy();
                    if (mappedBy != null && mappedBy.getStorage() instanceof MiddleTable) {
                        middleTable = mappedBy.<MiddleTable>getStorage().getInverse();
                    }
                }
                if (middleTable != null) {
                    MiddleTableOperator middleTableOperator = new MiddleTableOperator(
                            data.getSqlClient(),
                            con,
                            middleTable,
                            prop.getTargetType().getIdProp().getElementClass()
                    );
                    int rowCount = middleTableOperator.setTargetIds(
                            currentDraftSpi.__get(currentDraftSpi.__type().getIdProp().getName()),
                            associatedObjectIds
                    );
                    addOutput(middleTable.getTableName(), rowCount);
                }
                if (data.getAutoDetachingSet().contains(prop)) {
                    List<Object> targetIds = Queries.createQuery(data.getSqlClient(), prop.getTargetType(), (q, t) -> {
                        q.where(t
                                .join(prop.getMappedBy().getName())
                                .<Expression<Object>>get(prop.getDeclaringType().getIdProp().getName())
                                .notIn(associatedObjectIds)
                        );
                        return q.select(
                                t.<Expression<Object>>get(
                                        prop.getTargetType().getIdProp().getName()
                                )
                        );
                    }).execute(con);
                    Set<Object> removingTargetIds = new LinkedHashSet<>(targetIds);
                    removingTargetIds.removeAll(associatedObjectIds);
                    Deleter deleter = new Deleter(
                            new DeleteCommandImpl.Data(data.getSqlClient()),
                            con,
                            affectedRowCountMap
                    );
                    deleter.addPreHandleInput(prop.getTargetType(), removingTargetIds);
                    deleter.execute();
                }
            }
        }
    }

    private Object saveAssociatedObjectAndGetId(ImmutableProp prop, DraftSpi associatedDraftSpi) {
        if (isNonIdPropLoaded(associatedDraftSpi)) {
            AbstractSaveCommandImpl.Data associatedData =
                    new AbstractSaveCommandImpl.Data(data);
            associatedData.setMode(
                    data.getAutoAttachingSet().contains(prop) ?
                            AbstractSaveCommand.Mode.UPSERT :
                            AbstractSaveCommand.Mode.UPDATE_ONLY
            );
            Saver associatedSaver = new Saver(this, associatedData);
            associatedSaver.save(associatedDraftSpi);
        }
        return associatedDraftSpi.__get(associatedDraftSpi.__type().getIdProp().getName());
    }

    private void saveSelf(DraftSpi draftSpi) {
        if (data.getMode() == AbstractSaveCommand.Mode.INSERT_ONLY) {
            insert(draftSpi);
        } if (data.getMode() == AbstractSaveCommand.Mode.UPDATE_ONLY) {
            update(draftSpi);
        } else {
            ImmutableSpi existingSpi = findAssociatedObject(draftSpi);
            if (existingSpi == null) {
                insert(draftSpi);
            } else {
                update(draftSpi);
            }
        }
    }

    private void insert(DraftSpi draftSpi) {

        ImmutableType type = draftSpi.__type();
        IdGenerator idGenerator = data.getSqlClient().getIdGenerator(type.getJavaClass());
        Object id = draftSpi.__isLoaded(type.getIdProp().getName()) ?
                draftSpi.__get(type.getIdProp().getName()) :
                null;
        if (id == null) {
            if (idGenerator == null) {
                throw new ExecutionException(
                        "Cannot save \"" + type + "\" without id because id generator is not specified"
                );
            } else if (idGenerator instanceof SequenceIdGenerator) {
                String sql = data.getSqlClient().getDialect().getSelectIdFromSequenceSql(
                        ((SequenceIdGenerator)idGenerator).getSequenceName()
                );
                id = data.getSqlClient().getExecutor().execute(con, sql, Collections.emptyList(), stmt -> {
                    try (ResultSet rs = stmt.executeQuery()) {
                        rs.next();
                        return rs.getObject(1);
                    }
                });
                draftSpi = setDraftId(draftSpi, id);
            } else if (idGenerator instanceof UserIdGenerator) {
                id = ((UserIdGenerator)idGenerator).generate(type.getJavaClass());
                draftSpi = setDraftId(draftSpi, id);
            } else if (!(idGenerator instanceof IdentityIdGenerator)) {
                throw new ExecutionException(
                        "Illegal id generator type: \"" +
                                idGenerator.getClass().getName() +
                                "\", id generator must be sub type of \"" +
                                SequenceIdGenerator.class.getName() +
                                "\", \"" +
                                IdentityIdGenerator.class.getName() +
                                "\" or \"" +
                                UserIdGenerator.class.getName() +
                                "\""
                );
            }
        }

        List<ImmutableProp> props = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        for (ImmutableProp prop : draftSpi.__type().getProps().values()) {
            if (prop.getStorage() instanceof Column && draftSpi.__isLoaded(prop.getName())) {
                props.add(prop);
                values.add(draftSpi.__get(prop.getName()));
            }
        }
        if (type.getVersionProp() != null && !props.contains(type.getVersionProp())) {
            props.add(type.getVersionProp());
            values.add(0);
        }
        if (props.isEmpty()) {
            throw new ExecutionException("Cannot insert \"" + type + "\" without any properties");
        }
        SqlBuilder builder = new SqlBuilder(data.getSqlClient());
        builder
                .sql("insert into ")
                .sql(type.getTableName())
                .sql("(");
        String separator = "";
        for (ImmutableProp prop : props) {
            builder.sql(separator);
            separator = ", ";
            builder.sql(prop.<Column>getStorage().getName());
        }
        builder.sql(")");
        if (id != null && idGenerator instanceof IdentityIdGenerator) {
            String overrideIdentityIdSql = data.getSqlClient().getDialect().getOverrideIdentityIdSql();
            if (overrideIdentityIdSql != null) {
                builder.sql(" ").sql(overrideIdentityIdSql);
            }
        }
        builder.sql(" values(");
        separator = "";
        int size = values.size();
        for (int i = 0; i < size; i++) {
            builder.sql(separator);
            separator = ", ";
            Object value = values.get(i);
            if (value != null) {
                builder.variable(value);
            } else {
                builder.nullVariables(props.get(i).getElementClass());
            }
        }
        builder.sql(")");

        Tuple2<String, List<Object>> sqlResult = builder.build();
        int rowCount = data.getSqlClient().getExecutor().execute(
                con,
                sqlResult._1(),
                sqlResult._2(),
                PreparedStatement::executeUpdate
        );
        addOutput(type.getTableName(), rowCount);

        if (id == null) {
            id = data.getSqlClient().getExecutor().execute(
                    con,
                    data.getSqlClient().getDialect().getLastIdentitySql(),
                    Collections.emptyList(),
                    stmt -> {
                        try (ResultSet rs = stmt.executeQuery()) {
                            rs.next();
                            return rs.getObject(1);
                        }
                    }
            );
            draftSpi = setDraftId(draftSpi, id);
        }

        cache.save(draftSpi, true);
    }

    private void update(DraftSpi draftSpi) {

        ImmutableType type = draftSpi.__type();

        List<ImmutableProp> keyProps = new ArrayList<>(actualKeyProps(draftSpi));
        Key key = Key.of(data, draftSpi, false);
        List<Object> keyValues;
        if (key != null) {
            keyValues = key.toList();
        } else {
            if (!draftSpi.__isLoaded(type.getIdProp().getName())) {
                throw new ExecutionException(
                        "Cannot update \"" +
                                type +
                                "\" with neither id property nor key properties"
                );
            }
            keyValues = Collections.singletonList(draftSpi.__get(type.getIdProp().getName()));
        }

        List<ImmutableProp> updatedProps = new ArrayList<>();
        List<Object> updatedValues = new ArrayList<>();
        Integer version = null;

        for (ImmutableProp prop : type.getProps().values()) {
            if (prop.getStorage() instanceof Column && draftSpi.__isLoaded(prop.getName())) {
                if (prop.isVersion()) {
                    version = (Integer) draftSpi.__get(prop.getName());
                } else if (!keyProps.contains(prop)) {
                    updatedProps.add(prop);
                    updatedValues.add(draftSpi.__get(prop.getName()));
                }
            }
        }
        if (type.getVersionProp() != null && version == null) {
            throw new ExecutionException(
                    "Cannot update \"" +
                            type +
                            "\", the version property \"" +
                            type.getVersionProp() +
                            "\" is unloaded"
            );
        }
        if (updatedProps.isEmpty() && version == null) {
            return;
        }
        SqlBuilder builder = new SqlBuilder(data.getSqlClient());
        builder
                .sql("update ")
                .sql(type.getTableName())
                .sql(" set ");

        String separator = "";
        int updatedCount = updatedProps.size();
        for (int i = 0; i < updatedCount; i++) {
            builder.sql(separator);
            separator = ", ";
            builder
                    .sql(updatedProps.get(i).<Column>getStorage().getName())
                    .sql(" = ");
            Object updatedValue = updatedValues.get(i);
            if (updatedValue != null) {
                builder.variable(updatedValue);
            } else {
                builder.nullVariables(updatedProps.get(i).getElementClass());
            }
        }
        if (version != null) {
            String versionColumName = type.getVersionProp().<Column>getStorage().getName();
            builder
                    .sql(separator)
                    .sql(versionColumName)
                    .sql(" = ")
                    .sql(versionColumName)
                    .sql(" + 1");
        }
        builder.sql(" where ");

        separator = "";
        int keyCount = keyProps.size();
        for (int i = 0; i < keyCount; i++) {
            builder.sql(separator);
            separator = " and ";
            builder.
                    sql(keyProps.get(i).<Column>getStorage().getName())
                    .sql(" = ")
                    .variable(keyValues.get(i));
        }
        if (version != null) {
            builder
                    .sql(separator)
                    .sql(type.getVersionProp().<Column>getStorage().getName())
                    .sql(" = ")
                    .variable(version);
        }

        Tuple2<String, List<Object>> sqlResult = builder.build();
        int rowCount = data.getSqlClient().getExecutor().execute(
                con,
                sqlResult._1(),
                sqlResult._2(),
                PreparedStatement::executeUpdate
        );
        if (rowCount != 0) {
            addOutput(type.getTableName(), rowCount);
        }
        if (version != null) {
            draftSpi = increaseDraftVersion(draftSpi);
        }
        cache.save(draftSpi);
    }

    @SuppressWarnings("unchecked")
    private ImmutableSpi findAssociatedObject(DraftSpi example) {

        ImmutableSpi cached = cache.find(example);
        if (cached != null) {
            return cached;
        }

        ImmutableType type = example.__type();

        Collection<ImmutableProp> actualKeyProps = actualKeyProps(example);

        List<ImmutableSpi> rows = (List<ImmutableSpi>)Queries.createQuery(data.getSqlClient(), type, (q, table) -> {
            for (ImmutableProp keyProp : actualKeyProps) {
                q.where(
                        table.<Expression<Object>>get(keyProp.getName()).eq(
                                example.__get(keyProp.getName())
                        )
                );
            }
            return q.select(table);
        }).execute(con);

        ImmutableSpi spi = rows.isEmpty() ? null : rows.get(0);
        if (spi != null) {
            cache.save(spi, true);
        }
        return spi;
    }

    private Collection<ImmutableProp> actualKeyProps(ImmutableSpi spi) {

        ImmutableType type = spi.__type();
        ImmutableProp idProp = type.getIdProp();
        Object id = spi.__isLoaded(idProp.getName()) ?
                spi.__get(idProp.getName()) :
                null;

        if (id != null) {
            return Collections.singleton(idProp);
        }
        Set<ImmutableProp> keyProps = data.getKeyPropMultiMap().get(type);
        if (keyProps == null) {
            throw new ExecutionException(
                    "Cannot save \"" + type + "\" without id, " +
                            "key properties is not configured"
            );
        }
        return keyProps;
    }

    private void addOutput(String tableName, int affectedRowCount) {
        affectedRowCountMap.merge(tableName, affectedRowCount, Integer::sum);
    }

    private boolean isNonIdPropLoaded(ImmutableSpi spi) {
        boolean idPropLoaded = false;
        boolean nonIdPropLoaded = false;
        for (ImmutableProp prop : spi.__type().getProps().values()) {
            if (spi.__isLoaded(prop.getName())) {
                if (prop.isId()) {
                    idPropLoaded = true;
                } else {
                    nonIdPropLoaded = true;
                }
            }
        }
        if (nonIdPropLoaded) {
            ImmutableProp versionProp = spi.__type().getVersionProp();
            if (versionProp != null) {
                if (spi.__isLoaded(versionProp.getName())) {
                    throw new ExecutionException(
                            "Cannot save illegal entity object " +
                                    spi +
                                    " whose type is \"" +
                                    spi.__type() +
                                    "\", its version property \"" +
                                    versionProp.getName() +
                                    "\" must be loaded when there are some loaded non-id properties"
                    );
                }
            }
            if (!idPropLoaded) {
                Set<ImmutableProp> keyProps = data.getKeyPropMultiMap().get(spi.__type());
                for (ImmutableProp keyProp : keyProps) {
                    throw new ExecutionException(
                            "Cannot save illegal entity object " +
                                    spi +
                                    " whose type is \"" +
                                    spi.__type() +
                                    "\", key property \"" +
                                    keyProp +
                                    "\" must be loaded when id is unloaded"
                    );
                }
            }
        } else if (!idPropLoaded) {
            throw new ExecutionException(
                    "Cannot save illegal entity object " +
                            spi +
                            " whose type is \"" +
                            spi.__type() +
                            "\", no property is loaded"
            );
        }
        return nonIdPropLoaded;
    }

    private static DraftSpi setDraftId(DraftSpi spi, Object id) {
        ImmutableType type = spi.__type();
        ImmutableProp idProp = type.getIdProp();
        Object convertedId = Converts.tryConvert(id, idProp.getElementClass());
        if (convertedId == null) {
            throw new ExecutionException(
                    "The type of generated id does not match the property \"" + idProp + "\""
            );
        }
        return (DraftSpi) Internal.produce(type, spi, draft -> {
            ((DraftSpi)draft).__set(idProp.getName(), convertedId);
        });
    }

    private static DraftSpi increaseDraftVersion(DraftSpi spi) {
        ImmutableType type = spi.__type();
        ImmutableProp versionProp = type.getVersionProp();
        return (DraftSpi) Internal.produce(type, spi, draft -> {
            ((DraftSpi)draft).__set(
                    versionProp.getName(),
                    (Integer)((DraftSpi) draft).__get(versionProp.getName()) + 1
            );
        });
    }
}
