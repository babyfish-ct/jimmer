package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.Draft;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.meta.impl.RedirectedProp;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.DraftInterceptor;
import org.babyfish.jimmer.sql.OptimisticLockException;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.query.Queries;
import org.babyfish.jimmer.sql.ast.mutation.AffectedTable;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.ast.mutation.SimpleSaveResult;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.meta.*;
import org.babyfish.jimmer.sql.runtime.Converters;
import org.babyfish.jimmer.sql.runtime.ExecutionException;
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

class Saver {

    private final AbstractEntitySaveCommandImpl.Data data;

    private final Connection con;

    private final SaverCache cache;

    private final MutationTrigger trigger;

    private final Map<AffectedTable, Integer> affectedRowCountMap;

    private final String path;

    Saver(
            AbstractEntitySaveCommandImpl.Data data,
            Connection con
    ) {
        this(data, con, new SaverCache(data), new LinkedHashMap<>());
    }

    Saver(
            AbstractEntitySaveCommandImpl.Data data,
            Connection con,
            SaverCache cache,
            Map<AffectedTable, Integer> affectedRowCountMap) {
        this.data = data;
        this.con = con;
        this.cache = cache;
        this.trigger = data.getTriggers() != null ? new MutationTrigger() : null;
        this.affectedRowCountMap = affectedRowCountMap;
        this.path = "<root>";
    }

    Saver(Saver base, AbstractEntitySaveCommandImpl.Data data, String subPath) {
        this.data = data;
        this.con = base.con;
        this.cache = base.cache;
        this.trigger = base.trigger;
        this.affectedRowCountMap = base.affectedRowCountMap;
        this.path = base.path + '.' + subPath;
    }

    @SuppressWarnings("unchecked")
    public <E> SimpleSaveResult<E> save(E entity) {
        ImmutableType immutableType = ImmutableType.get(entity.getClass());
        E newEntity = (E)Internal.produce(
                immutableType,
                entity,
                draft -> {
                    saveImpl((DraftSpi) draft);
                },
                trigger == null ? null : trigger::prepareSubmit
        );
        if (trigger != null) {
            trigger.submit(data.getSqlClient(), con);
        }
        return new SimpleSaveResult<>(affectedRowCountMap, entity, newEntity);
    }

    private void saveImpl(DraftSpi draftSpi) {
        saveAssociations(draftSpi, ObjectType.EXISTING, true);
        ObjectType objectType = saveSelf(draftSpi);
        saveAssociations(draftSpi, objectType, false);
    }

    @SuppressWarnings("unchecked")
    private void saveAssociations(DraftSpi currentDraftSpi, ObjectType currentObjectType, boolean forParent) {

        ImmutableType currentType = currentDraftSpi.__type();

        for (ImmutableProp prop : currentType.getProps().values()) {
            if (prop.isAssociation(TargetLevel.ENTITY) &&
                    prop.getStorage() instanceof Column == forParent &&
                    currentDraftSpi.__isLoaded(prop.getId())
            ) {
                ImmutableType targetType = prop.getTargetType();
                int currentIdPropId = currentType.getIdProp().getId();
                Object currentId = currentDraftSpi.__isLoaded(currentIdPropId) ?
                        currentDraftSpi.__get(currentIdPropId) :
                        null;

                ImmutableProp mappedBy = prop.getMappedBy();
                ChildTableOperator childTableOperator = null;
                if (mappedBy != null && mappedBy.getStorage() instanceof Column) {
                    childTableOperator = new ChildTableOperator(
                            data.getSqlClient(),
                            con,
                            RedirectedProp.source(mappedBy, prop.getTargetType()),
                            cache,
                            trigger
                    );
                }
                Object associatedValue = currentDraftSpi.__get(prop.getId());
                Set<Object> associatedObjectIds = new LinkedHashSet<>();
                if (associatedValue != null) {
                    List<DraftSpi> associatedObjects =
                            associatedValue instanceof List<?> ?
                                    (List<DraftSpi>) associatedValue :
                                    Collections.singletonList((DraftSpi) associatedValue);
                    if (childTableOperator != null) {
                        int targetIdPropId = prop.getTargetType().getIdProp().getId();
                        List<Object> updatingTargetIds = new ArrayList<>();
                        for (DraftSpi associatedObject : associatedObjects) {
                            if (isNonIdPropLoaded(associatedObject, false)) {
                                associatedObject.__set(
                                        mappedBy.getId(),
                                        Internal.produce(currentType, null, backRef -> {
                                            ((DraftSpi) backRef).__set(currentIdPropId, currentId);
                                        })
                                );
                            } else {
                                updatingTargetIds.add(associatedObject.__get(targetIdPropId));
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
                }
                MiddleTableOperator middleTableOperator = MiddleTableOperator.tryGet(
                    data.getSqlClient(), con, prop, trigger
                );
                if (middleTableOperator != null) {
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
                    addOutput(AffectedTable.of(prop), rowCount);
                } else if (childTableOperator != null && currentObjectType != ObjectType.NEW) {
                    DissociateAction dissociateAction = data.getDissociateAction(prop.getMappedBy());
                    if (dissociateAction == DissociateAction.DELETE) {
                        List<Object> detachedTargetIds = childTableOperator.getDetachedChildIds(
                                currentId,
                                associatedObjectIds
                        );
                        Deleter deleter = new Deleter(
                                new DeleteCommandImpl.Data(data.getSqlClient(), data.dissociateActionMap()),
                                con,
                                cache,
                                trigger,
                                affectedRowCountMap
                        );
                        deleter.addPreHandleInput(prop.getTargetType(), detachedTargetIds);
                        deleter.execute(false);
                    } else if (dissociateAction == DissociateAction.SET_NULL) {
                        int rowCount = childTableOperator.unsetParent(currentId, associatedObjectIds);
                        addOutput(AffectedTable.of(targetType), rowCount);
                    } else {
                        throw new ExecutionException(
                                "Cannot disconnect child objects at the path \"" +
                                        path +
                                        "\" by the one-to-many association \"" +
                                        prop +
                                        "\" because the many-to-one property \"" +
                                        mappedBy +
                                        "\" is not configured as \"on delete set null\" or \"on delete cascade\"." +
                                        "There are two ways to resolve this issue, configure SaveCommand to automatically detach " +
                                        "disconnected child objects of the one-to-many property \"" +
                                        prop +
                                        "\", or set the delete action of the many-to-one property \"" +
                                        mappedBy +
                                        "\" to be \"CASCADE\"."
                        );
                    }
                }
            }
        }
    }

    private Object saveAssociatedObjectAndGetId(ImmutableProp prop, DraftSpi associatedDraftSpi) {
        if (isNonIdPropLoaded(associatedDraftSpi, true)) {
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
        return associatedDraftSpi.__get(associatedDraftSpi.__type().getIdProp().getId());
    }

    private ObjectType saveSelf(DraftSpi draftSpi) {

        if (cache.isSaved(draftSpi)) {
            return ObjectType.EXISTING;
        }

        if (data.getMode() == SaveMode.INSERT_ONLY) {
            if (trigger != null) {
                trigger.modifyEntityTable(null, draftSpi);
            }
            insert(draftSpi);
            return ObjectType.NEW;
        }

        if (trigger == null &&
                data.getMode() == SaveMode.UPDATE_ONLY &&
                draftSpi.__isLoaded(draftSpi.__type().getIdProp().getId())) {
            update(draftSpi, false);
            return ObjectType.EXISTING;
        }

        ImmutableSpi existingSpi = find(draftSpi);
        if (existingSpi != null) {
            boolean updated = false;
            int idPropId = draftSpi.__type().getIdProp().getId();
            if (draftSpi.__isLoaded(idPropId)) {
                updated = update(draftSpi, false);
            } else {
                draftSpi.__set(idPropId, existingSpi.__get(idPropId));
                updated = update(draftSpi, true);
            }
            if (updated && trigger != null) {
                trigger.modifyEntityTable(existingSpi, draftSpi);
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

        if (trigger != null) {
            trigger.modifyEntityTable(null, draftSpi);
        }
        insert(draftSpi);
        return ObjectType.NEW;
    }

    @SuppressWarnings("unchecked")
    private void insert(DraftSpi draftSpi) {

        callInterceptor(draftSpi, true);

        ImmutableType type = draftSpi.__type();
        IdGenerator idGenerator = data.getSqlClient().getIdGenerator(type.getJavaClass());
        Object id = draftSpi.__isLoaded(type.getIdProp().getId()) ?
                draftSpi.__get(type.getIdProp().getId()) :
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
                id = data.getSqlClient().getExecutor().execute(con, sql, Collections.emptyList(), ExecutionPurpose.MUTATE, null, stmt -> {
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
        if (type.getVersionProp() != null && !draftSpi.__isLoaded(type.getVersionProp().getId())) {
            draftSpi.__set(type.getVersionProp().getId(), 0);
        }

        List<ImmutableProp> props = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        for (ImmutableProp prop : draftSpi.__type().getProps().values()) {
            if (prop.getStorage() instanceof Column && draftSpi.__isLoaded(prop.getId())) {
                props.add(prop);
                Object value = draftSpi.__get(prop.getId());
                if (value != null && prop.isReference(TargetLevel.ENTITY)) {
                    value = ((ImmutableSpi) value).__get(prop.getTargetType().getIdProp().getId());
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
        SqlBuilder builder = new SqlBuilder(new AstContext(data.getSqlClient()));
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
                builder.nullVariable(props.get(i));
            }
        }
        builder.sql(")");

        Tuple2<String, List<Object>> sqlResult = builder.build();
        boolean generateKeys = id == null;
        Object insertedResult = data.getSqlClient().getExecutor().execute(
                con,
                sqlResult.get_1(),
                sqlResult.get_2(),
                ExecutionPurpose.MUTATE,
                generateKeys ?
                        (c, s) ->
                                c.prepareStatement(s, Statement.RETURN_GENERATED_KEYS) :
                        null,
                stmt -> {
                    if (generateKeys) {
                        int updateCount = stmt.executeUpdate();
                        Object generatedId;
                        try (ResultSet rs = stmt.getGeneratedKeys()) {
                            rs.next();
                            generatedId = rs.getObject(1);
                        }
                        return new Tuple2<>(updateCount, generatedId);
                    }
                    return stmt.executeUpdate();
                }
        );
        int rowCount = insertedResult instanceof Tuple2<?, ?> ?
                ((Tuple2<Integer, ?>)insertedResult).get_1() :
                (Integer)insertedResult;
        addOutput(AffectedTable.of(type), rowCount);

        if (insertedResult instanceof Tuple2<?, ?>) {
            id = ((Tuple2<?, Object>)insertedResult).get_2();
            setDraftId(draftSpi, id);
        }

        cache.save(draftSpi, true);
    }

    private boolean update(DraftSpi draftSpi, boolean excludeKeyProps) {

        callInterceptor(draftSpi, false);

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
            if (prop.getStorage() instanceof Column && draftSpi.__isLoaded(prop.getId())) {
                if (prop.isVersion()) {
                    version = (Integer) draftSpi.__get(prop.getId());
                } else if (!prop.isId() && !excludeProps.contains(prop)) {
                    updatedProps.add(prop);
                    Object value = draftSpi.__get(prop.getId());
                    if (value != null && prop.isReference(TargetLevel.ENTITY)) {
                        value = ((ImmutableSpi)value).__get(prop.getTargetType().getIdProp().getId());
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
            return false;
        }
        SqlBuilder builder = new SqlBuilder(new AstContext(data.getSqlClient()));
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
                builder.nullVariable(updatedProps.get(i));
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
                .variable(draftSpi.__get(type.getIdProp().getId()));
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
                sqlResult.get_1(),
                sqlResult.get_2(),
                ExecutionPurpose.MUTATE,
                null,
                PreparedStatement::executeUpdate
        );
        if (rowCount != 0) {
            addOutput(AffectedTable.of(type), rowCount);
            if (version != null) {
                increaseDraftVersion(draftSpi);
            }
            cache.save(draftSpi, true);
        } else if (version != null) {
            throw new OptimisticLockException(
                    type,
                    draftSpi.__get(type.getIdProp().getId()),
                    version,
                    path
            );
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private void callInterceptor(DraftSpi draftSpi, boolean insert) {
        ImmutableType type = draftSpi.__type();
        DraftInterceptor<?> interceptor = data.getSqlClient().getDraftInterceptor(type);
        if (interceptor != null) {
            int idPropId = type.getIdProp().getId();
            Object id = draftSpi.__isLoaded(idPropId) ?
                    draftSpi.__get(type.getIdProp().getId()) :
                    null;
            ((DraftInterceptor<Draft>) interceptor).beforeSave(draftSpi, insert);
            if (id != null) {
                if (!draftSpi.__isLoaded(idPropId)) {
                    throw new IllegalStateException("Draft interceptor cannot be used to unload id");
                }
                if (!id.equals(draftSpi.__get(idPropId))) {
                    throw new IllegalStateException("Draft interceptor cannot be used to change id");
                }
            }
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

        List<ImmutableSpi> rows = Internal.requiresNewDraftContext(ctx -> {
            List<ImmutableSpi> list = Queries.createQuery(data.getSqlClient(), type, ExecutionPurpose.MUTATE, true, (q, table) -> {
                for (ImmutableProp keyProp : actualKeyProps) {
                    if (keyProp.isReference(TargetLevel.ENTITY)) {
                        ImmutableProp targetIdProp = keyProp.getTargetType().getIdProp();
                        Expression<Object> targetIdExpression =
                                table
                                        .<Table<?>>join(keyProp.getName())
                                        .get(targetIdProp.getName());
                        ImmutableSpi target = (ImmutableSpi) example.__get(keyProp.getId());
                        if (target != null) {
                            q.where(targetIdExpression.eq(target.__get(targetIdProp.getId())));
                        } else {
                            q.where(targetIdExpression.isNull());
                        }
                    } else {
                        Object value = example.__get(keyProp.getId());
                        if (value != null) {
                            q.where(table.<Expression<Object>>get(keyProp.getName()).eq(value));
                        } else {
                            q.where(table.<Expression<Object>>get(keyProp.getName()).isNull());
                        }
                    }
                }
                if (trigger != null) {
                    return q.select((Table<ImmutableSpi>)table);
                }
                return q.select(
                        ((Table<ImmutableSpi>)table).fetch(
                                IdAndKeyFetchers.getFetcher(type)
                        )
                );
            }).forUpdate().execute(con);
            return ctx.resolveList(list);
        });

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
            cache.save(spi, false);
        }
        return spi;
    }

    private Collection<ImmutableProp> actualKeyProps(ImmutableSpi spi) {

        ImmutableType type = spi.__type();
        ImmutableProp idProp = type.getIdProp();
        Object id = spi.__isLoaded(idProp.getId()) ?
                spi.__get(idProp.getId()) :
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

    private boolean isNonIdPropLoaded(ImmutableSpi spi, boolean validate) {
        boolean idPropLoaded = false;
        boolean nonIdPropLoaded = false;
        for (ImmutableProp prop : spi.__type().getProps().values()) {
            if (spi.__isLoaded(prop.getId())) {
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
                if (validate && !spi.__isLoaded(keyProp.getId())) {
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
        } else if (validate && !idPropLoaded) {
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
        Object convertedId = Converters.tryConvert(id, idProp.getElementClass());
        if (convertedId == null) {
            throw new ExecutionException(
                    "The type of generated id does not match the property \"" + idProp + "\""
            );
        }
        spi.__set(idProp.getId(), convertedId);
    }

    private static void increaseDraftVersion(DraftSpi spi) {
        ImmutableType type = spi.__type();
        ImmutableProp versionProp = type.getVersionProp();
        spi.__set(
                versionProp.getId(),
                (Integer)spi.__get(versionProp.getId()) + 1
        );
    }

    private enum ObjectType {
        UNKNOWN,
        NEW,
        EXISTING
    }
}
