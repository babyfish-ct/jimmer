package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.impl.query.Queries;
import org.babyfish.jimmer.sql.ast.mutation.AffectedTable;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.ast.mutation.SimpleSaveResult;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.meta.*;
import org.babyfish.jimmer.sql.runtime.Converts;
import org.babyfish.jimmer.sql.runtime.ExecutionException;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

class Saver {

    private AbstractEntitySaveCommandImpl.Data data;

    private Connection con;

    private ImmutableCache cache;

    private Map<AffectedTable, Integer> affectedRowCountMap;

    private String path;

    Saver(
            AbstractEntitySaveCommandImpl.Data data,
            Connection con
    ) {
        this(data, con, new ImmutableCache(data), new LinkedHashMap<>());
    }

    Saver(
            AbstractEntitySaveCommandImpl.Data data,
            Connection con,
            ImmutableCache cache,
            Map<AffectedTable, Integer> affectedRowCountMap) {
        this.data = data;
        this.con = con;
        this.cache = cache;
        this.affectedRowCountMap = affectedRowCountMap;
        this.path = "<root>";
    }

    Saver(Saver base, AbstractEntitySaveCommandImpl.Data data, String subPath) {
        this.data = data;
        this.con = base.con;
        this.cache = base.cache;
        this.affectedRowCountMap = base.affectedRowCountMap;
        this.path = base.path + '.' + subPath;
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
        saveAssociations(draftSpi, ObjectType.EXISTING, true);
        ObjectType objectType = saveSelf(draftSpi);
        saveAssociations(draftSpi, objectType, false);
    }

    @SuppressWarnings("unchecked")
    private void saveAssociations(DraftSpi currentDraftSpi, ObjectType currentObjectType, boolean forParent) {
        for (ImmutableProp prop : currentDraftSpi.__type().getProps().values()) {
            if (prop.isAssociation() &&
                    prop.getStorage() instanceof Column == forParent &&
                    currentDraftSpi.__isLoaded(prop.getName())
            ) {
                ImmutableType targetType = prop.getTargetType();
                ImmutableType currentType = currentDraftSpi.__type();
                String currentIdPropName = currentType.getIdProp().getName();
                Object currentId = currentDraftSpi.__isLoaded(currentIdPropName) ?
                        currentDraftSpi.__get(currentIdPropName) :
                        null;

                ImmutableProp mappedBy = prop.getMappedBy();
                ChildTableOperator childTableOperator = null;
                if (mappedBy != null && mappedBy.getStorage() instanceof Column) {
                    childTableOperator = new ChildTableOperator(
                            data.getSqlClient(),
                            con,
                            mappedBy
                    );
                }
                Object associatedValue = currentDraftSpi.__get(prop.getName());
                Set<Object> associatedObjectIds = new LinkedHashSet<>();
                if (associatedValue instanceof List<?>) {
                    List<DraftSpi> associatedObjects = (List<DraftSpi>) associatedValue;
                    if (childTableOperator != null) {
                        String targetIdPropName = prop.getTargetType().getIdProp().getName();
                        Iterator<DraftSpi> itr = new ArrayList<>(associatedObjects).iterator();
                        List<Object> updatingTargetIds = new ArrayList<>();
                        while (itr.hasNext()) {
                            DraftSpi associatedObject = itr.next();
                            if (isNonIdPropLoaded(associatedObject)) {
                                associatedObject.__set(
                                        mappedBy.getName(),
                                        Internal.produce(currentType, null, backRef -> {
                                            ((DraftSpi) backRef).__set(currentIdPropName, currentId);
                                        })
                                );
                            } else {
                                updatingTargetIds.add(associatedObject.__get(targetIdPropName));
                                itr.remove();
                            }
                        }
                        if (!updatingTargetIds.isEmpty()) {
                            int rowCount = childTableOperator.setParent(currentId, updatingTargetIds);
                            addOutput(AffectedTable.of(targetType), rowCount);
                        }
                    }
                    for (DraftSpi associatedObject : associatedObjects) {
                        associatedObjectIds.add(saveAssociatedObjectAndGetId(prop, associatedObject));
                    }
                } else {
                    DraftSpi associatedObject = (DraftSpi) associatedValue;
                    associatedObjectIds.add(saveAssociatedObjectAndGetId(prop, associatedObject));
                }
                ImmutableProp middleTableProp = null;
                MiddleTable middleTable = null;
                if (prop.getStorage() instanceof MiddleTable) {
                    middleTableProp = prop;
                    middleTable = middleTableProp.getStorage();
                } else {
                    if (mappedBy != null && mappedBy.getStorage() instanceof MiddleTable) {
                        middleTableProp = mappedBy;
                        middleTable = middleTableProp.<MiddleTable>getStorage().getInverse();
                    }
                }
                if (middleTable != null) {
                    MiddleTableOperator middleTableOperator = new MiddleTableOperator(
                            data.getSqlClient(),
                            con,
                            middleTable,
                            prop.getTargetType().getIdProp().getElementClass()
                    );
                    int rowCount;
                    if (currentObjectType == ObjectType.NEW) {
                        rowCount = middleTableOperator.addTargetIds(
                                currentId,
                                associatedObjectIds
                        );
                    } else {
                        rowCount = middleTableOperator.setTargetIds(
                                currentId,
                                associatedObjectIds
                        );
                    }
                    addOutput(AffectedTable.middle(middleTableProp), rowCount);
                } else if (childTableOperator != null && currentObjectType != ObjectType.NEW) {
                    if (data.isAutoDetachingProp(prop)) {
                        List<Object> detachedTargetIds = childTableOperator.getDetachedChildIds(
                                currentId,
                                associatedObjectIds
                        );
                        Deleter deleter = new Deleter(
                                new DeleteCommandImpl.Data(data.getSqlClient()),
                                con,
                                affectedRowCountMap
                        );
                        deleter.addPreHandleInput(prop.getTargetType(), detachedTargetIds);
                        deleter.execute();
                    } else {
                        if (!mappedBy.isNullable()) {
                            throw new ExecutionException(
                                    "Cannot disconnect child objects at the path \"" +
                                            path +
                                            "\" by the one-to-many association \"" +
                                            prop +
                                            "\" because the many-to-one property \"" +
                                            mappedBy +
                                            "\" is not nullable." +
                                            "There are two ways to resolve this issue, configure SaveCommand to automatically detach " +
                                            "disconnected child objects of the one-to-many property \"" +
                                            prop +
                                            "\", or set the delete action of the many-to-one property \"" +
                                            mappedBy +
                                            "\" to be \"CASCADE\"."
                            );
                        }
                        int rowCount = childTableOperator.unsetParent(currentId, associatedObjectIds);
                        addOutput(AffectedTable.of(targetType), rowCount);
                    }
                }
            }
        }
    }

    private Object saveAssociatedObjectAndGetId(ImmutableProp prop, DraftSpi associatedDraftSpi) {
        if (isNonIdPropLoaded(associatedDraftSpi)) {
            AbstractEntitySaveCommandImpl.Data associatedData =
                    new AbstractEntitySaveCommandImpl.Data(data);
            associatedData.setMode(
                    data.isAutoAttachingProp(prop) ?
                            SaveMode.UPSERT :
                            SaveMode.UPDATE_ONLY
            );
            Saver associatedSaver = new Saver(this, associatedData, prop.getName());
            associatedSaver.saveImpl(associatedDraftSpi);
        }
        return associatedDraftSpi.__get(associatedDraftSpi.__type().getIdProp().getName());
    }

    private ObjectType saveSelf(DraftSpi draftSpi) {

        if (data.getMode() == SaveMode.INSERT_ONLY) {
            insert(draftSpi);
            return ObjectType.NEW;
        }

        if (data.getMode() == SaveMode.UPDATE_ONLY &&
                draftSpi.__isLoaded(draftSpi.__type().getIdProp().getName())) {
            update(draftSpi, false);
            return ObjectType.EXISTING;
        }

        ImmutableSpi existingSpi = find(draftSpi);
        if (existingSpi != null) {
            String idPropName = draftSpi.__type().getIdProp().getName();
            if (draftSpi.__isLoaded(idPropName)) {
                update(draftSpi, false);
            } else {
                draftSpi.__set(idPropName, existingSpi.__get(idPropName));
                update(draftSpi, true);
            }
            return ObjectType.EXISTING;
        }
        if (data.getMode() == SaveMode.UPDATE_ONLY) {
            throw new ExecutionException(
                    "Cannot insert object into path \"" +
                            path +
                            "\" because insert operation for this path is disabled"
            );
        }
        insert(draftSpi);
        return ObjectType.NEW;
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
                        "Cannot save \"" +
                                type + "\" " +
                                "without id into path \"" +
                                path +
                                "\" because id generator is not specified"
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
                setDraftId(draftSpi, id);
            } else if (idGenerator instanceof UserIdGenerator) {
                id = ((UserIdGenerator)idGenerator).generate(type.getJavaClass());
                setDraftId(draftSpi, id);
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
        if (type.getVersionProp() != null && !draftSpi.__isLoaded(type.getVersionProp().getName())) {
            draftSpi.__set(type.getVersionProp().getName(), 0);
        }

        List<ImmutableProp> props = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        for (ImmutableProp prop : draftSpi.__type().getProps().values()) {
            if (prop.getStorage() instanceof Column && draftSpi.__isLoaded(prop.getName())) {
                props.add(prop);
                Object value = draftSpi.__get(prop.getName());
                if (value != null && prop.isReference()) {
                    value = ((ImmutableSpi)value).__get(prop.getTargetType().getIdProp().getName());
                }
                values.add(value);
            }
        }
        if (props.isEmpty()) {
            throw new ExecutionException(
                    "Cannot insert \"" +
                            type +
                            "\" into path \"" +
                            path +
                            "\" without any properties"
            );
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
                builder.nullVariable(props.get(i).getElementClass());
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
        addOutput(AffectedTable.of(type), rowCount);

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
            setDraftId(draftSpi, id);
        }

        cache.save(draftSpi, true);
    }

    private void update(DraftSpi draftSpi, boolean excludeKeyProps) {

        ImmutableType type = draftSpi.__type();

        Set<ImmutableProp> excludeProps = null;
        if (excludeKeyProps) {
            excludeProps = data.getKeyProps(type);
        }
        if (excludeProps == null) {
            excludeProps = Collections.emptySet();
        }

        List<ImmutableProp> updatedProps = new ArrayList<>();
        List<Object> updatedValues = new ArrayList<>();
        Integer version = null;

        for (ImmutableProp prop : type.getProps().values()) {
            if (prop.getStorage() instanceof Column && draftSpi.__isLoaded(prop.getName())) {
                if (prop.isVersion()) {
                    version = (Integer) draftSpi.__get(prop.getName());
                } else if (!prop.isId() && !excludeProps.contains(prop)) {
                    updatedProps.add(prop);
                    Object value = draftSpi.__get(prop.getName());
                    if (value != null && prop.isReference()) {
                        value = ((ImmutableSpi)value).__get(prop.getTargetType().getIdProp().getName());
                    }
                    updatedValues.add(value);
                }
            }
        }
        if (type.getVersionProp() != null && version == null) {
            throw new ExecutionException(
                    "Cannot update \"" +
                            type +
                            "\" at the path \"" +
                            path +
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
                builder.nullVariable(updatedProps.get(i).getElementClass());
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

        builder.
                sql(type.getIdProp().<Column>getStorage().getName())
                .sql(" = ")
                .variable(draftSpi.__get(type.getIdProp().getName()));
        if (version != null) {
            builder
                    .sql(" and ")
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
            addOutput(AffectedTable.of(type), rowCount);
            if (version != null) {
                increaseDraftVersion(draftSpi);
            }
            cache.save(draftSpi);
        }
    }

    @SuppressWarnings("unchecked")
    private ImmutableSpi find(DraftSpi example) {

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
            return q.select(
                    ((Table<ImmutableSpi>)table).fetch(
                            IdAndKeyFetchers.getFetcher(type)
                    )
            );
        }).execute(con);

        if (rows.size() > 1) {
            throw new ExecutionException(
                    "Key properties " +
                            actualKeyProps +
                            " cannot guarantee uniqueness at the path \"" +
                            path +
                            "\""
            );
        }

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
        Set<ImmutableProp> keyProps = data.getKeyProps(type);
        if (keyProps == null) {
            throw new ExecutionException(
                    "Cannot save \"" +
                            type +
                            "\" without id into the path \"" +
                            path +
                            "\", " +
                            "key properties is not configured"
            );
        }
        return keyProps;
    }

    private void addOutput(AffectedTable affectTable, int affectedRowCount) {
        if (affectedRowCount != 0) {
            affectedRowCountMap.merge(affectTable, affectedRowCount, Integer::sum);
        }
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
        if (nonIdPropLoaded && !idPropLoaded) {
            Set<ImmutableProp> keyProps = data.getKeyProps(spi.__type());
            for (ImmutableProp keyProp : keyProps) {
                if (!spi.__isLoaded(keyProp.getName())) {
                    throw new ExecutionException(
                            "Cannot save illegal entity object " +
                                    spi +
                                    " whose type is \"" +
                                    spi.__type() +
                                    "\" into the path \"" +
                                    path +
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
                            "\" into the path \"" +
                            path +
                            "\", no property is loaded"
            );
        }
        return nonIdPropLoaded;
    }

    private static void setDraftId(DraftSpi spi, Object id) {
        ImmutableType type = spi.__type();
        ImmutableProp idProp = type.getIdProp();
        Object convertedId = Converts.tryConvert(id, idProp.getElementClass());
        if (convertedId == null) {
            throw new ExecutionException(
                    "The type of generated id does not match the property \"" + idProp + "\""
            );
        }
        spi.__set(idProp.getName(), convertedId);
    }

    private static void increaseDraftVersion(DraftSpi spi) {
        ImmutableType type = spi.__type();
        ImmutableProp versionProp = type.getVersionProp();
        spi.__set(
                versionProp.getName(),
                (Integer)spi.__get(versionProp.getName()) + 1
        );
    }

    private enum ObjectType {
        UNKNOWN,
        NEW,
        EXISTING
    }
}
