package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.Draft;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.LogicalDeletedInfo;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.meta.impl.DatabaseIdentifiers;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.*;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.PropExpression;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.query.Queries;
import org.babyfish.jimmer.sql.ast.mutation.AffectedTable;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.ast.mutation.SimpleSaveResult;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.fetcher.impl.FetcherImpl;
import org.babyfish.jimmer.sql.meta.*;
import org.babyfish.jimmer.sql.runtime.*;

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

    private final boolean triggerSubmitImmediately;

    private final Map<AffectedTable, Integer> affectedRowCountMap;

    private final SavePath path;

    private boolean triggerSubmitted;

    Saver(
            AbstractEntitySaveCommandImpl.Data data,
            Connection con,
            ImmutableType type
    ) {
        this(data, con, type, new SaverCache(data), true, new LinkedHashMap<>());
    }

    Saver(
            AbstractEntitySaveCommandImpl.Data data,
            Connection con,
            ImmutableType type,
            SaverCache cache,
            boolean triggerSubmitImmediately,
            Map<AffectedTable, Integer> affectedRowCountMap
    ) {
        this.data = data;
        this.con = con;
        this.cache = cache;
        this.trigger = data.getTriggers() != null ? new MutationTrigger() : null;
        this.triggerSubmitImmediately = triggerSubmitImmediately && this.trigger != null;
        this.affectedRowCountMap = affectedRowCountMap;
        this.path = SavePath.root(type);
    }

    Saver(Saver base, AbstractEntitySaveCommandImpl.Data data, ImmutableProp prop) {
        this.data = data;
        this.con = base.con;
        this.cache = base.cache;
        this.trigger = base.trigger;
        this.triggerSubmitImmediately = this.trigger != null;
        this.affectedRowCountMap = base.affectedRowCountMap;
        this.path = base.path.to(prop);
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
        if (triggerSubmitImmediately) {
            submitTrigger();
        }
        return new SimpleSaveResult<>(affectedRowCountMap, entity, newEntity);
    }

    public void submitTrigger() {
        if (trigger != null && !triggerSubmitted) {
            trigger.submit(data.getSqlClient(), con);
            triggerSubmitted = true;
        }
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
                    prop.getStorage() instanceof ColumnDefinition == forParent &&
                    currentDraftSpi.__isLoaded(prop.getId())
            ) {
                ImmutableType targetType = prop.getTargetType();
                if (prop.isRemote() && prop.getMappedBy() != null) {
                    throw new SaveException(
                            SaveErrorCode.REVERSED_REMOTE_ASSOCIATION,
                            path,
                            "The property \"" +
                                    prop +
                                    "\" which is reversed(with `mappedBy`) remote(across different microservices) association " +
                                    "cannot be supported by save command"
                    );
                }
                int currentIdPropId = currentType.getIdProp().getId();
                Object currentId = currentDraftSpi.__isLoaded(currentIdPropId) ?
                        currentDraftSpi.__get(currentIdPropId) :
                        null;

                ImmutableProp mappedBy = prop.getMappedBy();
                ChildTableOperator childTableOperator = null;
                if (!prop.isRemote() && mappedBy != null && mappedBy.getStorage() instanceof ColumnDefinition) {
                    childTableOperator = new ChildTableOperator(
                            data.getSqlClient(),
                            con,
                            mappedBy,
                            data.isPessimisticLockRequired(),
                            cache,
                            trigger
                    );
                }
                Object associatedValue = currentDraftSpi.__get(prop.getId());
                Set<Object> associatedObjectIds = new LinkedHashSet<>();
                if (associatedValue == null) {
                    if (prop.isInputNotNull()) {
                        throw new SaveException(
                                SaveErrorCode.NULL_TARGET,
                                path,
                                "The association \"" +
                                        prop +
                                        "\" cannot be null, because that association is decorated by \"@" +
                                        (prop.getAnnotation(ManyToOne.class) != null ? ManyToOne.class : OneToOne.class).getName() +
                                        "\" whose `inputNotNull` is true"
                        );
                    }
                } else {
                    List<DraftSpi> associatedObjects =
                            associatedValue instanceof List<?> ?
                                    (List<DraftSpi>) associatedValue :
                                    Collections.singletonList((DraftSpi) associatedValue);
                    List<Object> idOnlyTargetIds = Collections.emptyList();
                    if (data.isAutoCheckingProp(prop) || childTableOperator != null) {
                        int targetIdPropId = prop.getTargetType().getIdProp().getId();
                        idOnlyTargetIds = new ArrayList<>();
                        for (DraftSpi associatedObject : associatedObjects) {
                            if (!isNonIdPropLoaded(associatedObject, false)) {
                                idOnlyTargetIds.add(associatedObject.__get(targetIdPropId));
                            } else if (prop.isRemote()) {
                                throw new SaveException(
                                        SaveErrorCode.LONG_REMOTE_ASSOCIATION,
                                        path,
                                        "The property \"" +
                                                prop +
                                                "\" is remote(across different microservices) association, " +
                                                "but it has associated object which is not id-only"
                                );
                            }
                        }
                    }
                    if (data.isAutoCheckingProp(prop)) {
                        validateIdOnlyTargetIds(prop, idOnlyTargetIds);
                    }
                    if (childTableOperator != null) {
                        for (DraftSpi associatedObject : associatedObjects) {
                            if (isNonIdPropLoaded(associatedObject, false)) {
                                associatedObject.__set(
                                        mappedBy.getId(),
                                        Internal.produce(currentType, null, backRef -> {
                                            ((DraftSpi) backRef).__set(currentIdPropId, currentId);
                                        })
                                );
                            }
                        }
                        if (!idOnlyTargetIds.isEmpty()) {
                            int rowCount = childTableOperator.setParent(currentId, idOnlyTargetIds);
                            addOutput(AffectedTable.of(targetType), rowCount);
                        }
                    }
                    for (DraftSpi associatedObject : associatedObjects) {
                        associatedObjectIds.add(saveAssociatedObjectAndGetId(prop, associatedObject));
                    }
                }
                if (childTableOperator != null && currentObjectType != ObjectType.NEW && !data.isAppendOnly(prop)) {
                    DissociateAction dissociateAction = data.getDissociateAction(prop.getMappedBy());
                    if (dissociateAction == DissociateAction.DELETE) {
                        List<Object> detachedTargetIds = childTableOperator.getDetachedChildIds(
                                currentId,
                                associatedObjectIds
                        );
                        Deleter deleter = new Deleter(
                                new DeleteCommandImpl.Data(
                                        data.getSqlClient(),
                                        data.getDeleteMode(),
                                        data.dissociateActionMap()
                                ),
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
                        if (childTableOperator.exists(currentId, associatedObjectIds)) {
                            throw new SaveException(
                                    SaveErrorCode.CANNOT_DISSOCIATE_TARGETS,
                                    path.to(prop),
                                    "Cannot dissociate child objects because the dissociation action of the many-to-one property \"" +
                                            mappedBy +
                                            "\" is not configured as \"set null\" or \"cascade\". " +
                                            "There are two ways to resolve this issue: Decorate the many-to-one property \"" +
                                            mappedBy +
                                            "\" by @" +
                                            OnDissociate.class.getName() +
                                            " whose argument is `DissociateAction.SET_NULL` or `DissociateAction.DELETE` " +
                                            ", or use save command's runtime configuration to override it"
                            );
                        }
                    }
                }
                MiddleTableOperator middleTableOperator = MiddleTableOperator.tryGet(
                    data.getSqlClient(), con, prop, trigger
                );
                if (middleTableOperator != null) {
                    int rowCount;
                    if (currentObjectType == ObjectType.NEW || data.isAppendOnly(prop)) {
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
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void validateIdOnlyTargetIds(ImmutableProp prop, List<Object> targetIds) {
        if (targetIds.isEmpty()) {
            return;
        }
        Set<Object> illegalTargetIds = new LinkedHashSet<>(targetIds);
        if (prop.isRemote()) {
            int targetIdPropId = prop.getTargetType().getIdProp().getId();
            List<ImmutableSpi> targets;
            try {
                targets = data
                        .getSqlClient()
                        .getMicroServiceExchange()
                        .findByIds(
                                prop.getTargetType().getMicroServiceName(),
                                targetIds,
                                new FetcherImpl<>((Class<ImmutableSpi>) (prop.getTargetType().getJavaClass()))
                        );
            } catch (Exception ex) {
                throw new SaveException(
                        SaveErrorCode.FAILED_REMOTE_VALIDATION,
                        path,
                        "Cannot validate the id-only associated objects of remote association \"" +
                                prop +
                                "\""
                );
            }
            for (ImmutableSpi target : targets) {
                illegalTargetIds.remove(target.__get(targetIdPropId));
            }
        } else {
            List<Object> existingTargetIds = Queries
                    .createQuery(
                            data.getSqlClient(),
                            prop.getTargetType(),
                            ExecutionPurpose.MUTATE,
                            true,
                            (q, t) -> {
                                PropExpression<Object> idExpr = t.get(prop.getTargetType().getIdProp().getName());
                                q.where(idExpr.in(targetIds));
                                return q.select(idExpr);
                            }
                    ).execute(con);
            illegalTargetIds.removeAll(new HashSet<>(existingTargetIds));
        }
        if (!illegalTargetIds.isEmpty()) {
            throw new SaveException(
                    SaveErrorCode.ILLEGAL_TARGET_ID,
                    path.to(prop),
                    "Illegal ids: " + illegalTargetIds
            );
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
            Saver associatedSaver = new Saver(this, associatedData, prop);
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
            boolean updated;
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
            if (path.getParent() == null) {
                addOutput(AffectedTable.of(draftSpi.__type()), 0);
                return ObjectType.UNKNOWN;
            }
            String guide;
            if (path.getType().isKotlinClass()) {
                guide = "call `setAutoAttaching(" +
                        path.getProp().getDeclaringType().getJavaClass().getSimpleName() +
                        "::" +
                        path.getProp().getName() +
                        ")` or `setAutoAttachingAll()` of the save command";
            } else {
                guide = "call `setAutoAttaching(" +
                        path.getProp().getDeclaringType().getJavaClass().getSimpleName() +
                        "Props." +
                        DatabaseIdentifiers.databaseIdentifier(path.getProp().getName()) +
                        ")` or `setAutoAttachingAll()` of the save command";
            }
            throw new SaveException(
                    SaveErrorCode.CANNOT_CREATE_TARGET,
                    path,
                    "Cannot insert object because insert operation for this path is disabled, please " + guide
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
                throw new SaveException(
                        SaveErrorCode.NO_ID_GENERATOR,
                        path,
                        "Cannot save \"" +
                                type + "\" " +
                                "without id because id generator is not specified"
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
                id = ((UserIdGenerator<?>)idGenerator).generate(type.getJavaClass());
                setDraftId(draftSpi, id);
            } else if (!(idGenerator instanceof IdentityIdGenerator)) {
                throw new SaveException(
                        SaveErrorCode.ILLEGAL_ID_GENERATOR,
                        path,
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
            if (prop.getStorage() instanceof ColumnDefinition && draftSpi.__isLoaded(prop.getId())) {
                props.add(prop);
                Object value = draftSpi.__get(prop.getId());
                if (value != null) {
                    if (prop.isReference(TargetLevel.ENTITY)) {
                        value = ((ImmutableSpi) value).__get(prop.getTargetType().getIdProp().getId());
                    } else {
                        ScalarProvider<Object, Object> scalarProvider = data.getSqlClient().getScalarProvider(prop);
                        if (scalarProvider != null) {
                            try {
                                value = scalarProvider.toSql(value);
                            } catch (Exception ex) {
                                throw new ExecutionException(
                                        "Cannot convert the value of \"" +
                                                prop +
                                                "\" by the scalar provider \"" +
                                                scalarProvider.getClass().getName() +
                                                "\"",
                                        ex
                                );
                            }
                        }
                    }
                }
                values.add(value);
            }
        }
        if (props.isEmpty()) {
            throw new SaveException(
                    SaveErrorCode.NO_NON_ID_PROPS,
                    path,
                    "Cannot insert \"" +
                            type +
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
            builder.sql(prop.<ColumnDefinition>getStorage());
        }
        builder.sql(")");
        if (id != null && idGenerator instanceof IdentityIdGenerator) {
            String overrideIdentityIdSql = data.getSqlClient().getDialect().getOverrideIdentityIdSql();
            if (overrideIdentityIdSql != null) {
                builder.sql(" ").sql(overrideIdentityIdSql);
            }
        }
        builder.sql(" values").enterTuple();
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
        builder.leaveTuple();

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
            if (prop.getStorage() instanceof ColumnDefinition && draftSpi.__isLoaded(prop.getId())) {
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
            throw new SaveException(
                    SaveErrorCode.NO_VERSION,
                    path,
                    "Cannot update \"" +
                            type +
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
            builder.assignment(updatedProps.get(i), updatedValues.get(i));
        }
        if (version != null) {
            String versionColumName = type.getVersionProp().<SingleColumn>getStorage().getName();
            builder
                    .sql(separator)
                    .sql(versionColumName)
                    .sql(" = ")
                    .sql(versionColumName)
                    .sql(" + 1");
        }
        builder.sql(" where ");

        builder.
                sql(null, type.getIdProp().getStorage(), true)
                .sql(" = ")
                .variable(draftSpi.__get(type.getIdProp().getId()));
        if (version != null) {
            builder
                    .sql(" and ")
                    .sql(type.getVersionProp().<SingleColumn>getStorage().getName())
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
            throw new SaveException(
                    SaveErrorCode.ILLEGAL_VERSION,
                    path,
                    "Cannot update the entity whose type is \"" +
                            type +
                            "\", id is \"" +
                            draftSpi.__get(type.getIdProp().getId()) +
                            "\" and version is \"" +
                            version +
                            "\""
            );
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private void callInterceptor(DraftSpi draftSpi, boolean insert) {
        ImmutableType type = draftSpi.__type();
        LogicalDeletedInfo info = type.getLogicalDeletedInfo();
        if (info != null) {
            draftSpi.__set(info.getProp().getId(), info.getRestoredValue());
        }
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
        ImmutableProp prop = path.getProp();
        boolean requiresKey = prop != null && !data.isAppendOnly(prop);
        ImmutableSpi cached;
        try {
            cached = cache.find(example, requiresKey);
        } catch (IllegalArgumentException ex) {
            throw new SaveException(SaveErrorCode.NO_KEY_PROPS, path, ex.getMessage());
        }
        if (cached != null) {
            return cached;
        }

        ImmutableType type = example.__type();
        Collection<ImmutableProp> actualKeyProps = actualKeyProps(example, requiresKey);
        if (actualKeyProps == null || actualKeyProps.isEmpty()) {
            return null;
        }

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
            }).forUpdate(data.isPessimisticLockRequired()).execute(con);
            return ctx.resolveList(list);
        });

        if (rows.size() > 1) {
            throw new SaveException(
                    SaveErrorCode.KEY_NOT_UNIQUE,
                    path,
                    "Key properties " +
                            actualKeyProps +
                            " cannot guarantee uniqueness under that path"
            );
        }

        ImmutableSpi spi = rows.isEmpty() ? null : rows.get(0);
        if (spi != null) {
            cache.save(spi, false);
        }
        return spi;
    }

    private Collection<ImmutableProp> actualKeyProps(ImmutableSpi spi, boolean requiresKey) {

        ImmutableType type = spi.__type();
        ImmutableProp idProp = type.getIdProp();
        Object id = spi.__isLoaded(idProp.getId()) ?
                spi.__get(idProp.getId()) :
                null;

        if (id != null) {
            return Collections.singleton(idProp);
        }
        Set<ImmutableProp> keyProps = data.getKeyProps(type);
        if (keyProps == null && requiresKey) {
            throw new SaveException(
                    SaveErrorCode.NO_KEY_PROPS,
                    path,
                    "Cannot save \"" +
                            type +
                            "\" that have no properties decorated by \"@" +
                            Key.class.getName() +
                            "\""
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
                    throw new SaveException(
                            SaveErrorCode.NEITHER_ID_NOR_KEY,
                            path,
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
        } else if (validate && !idPropLoaded) {
            throw new SaveException(
                    SaveErrorCode.NEITHER_ID_NOR_KEY,
                    path,
                    "Cannot save illegal entity object " +
                            spi +
                            " whose type is \"" +
                            spi.__type() +
                            "\", neither id nor key is specified"
            );
        }
        return nonIdPropLoaded;
    }

    private void setDraftId(DraftSpi spi, Object id) {
        ImmutableType type = spi.__type();
        ImmutableProp idProp = type.getIdProp();
        Object convertedId = Converters.tryConvert(id, idProp.getElementClass());
        if (convertedId == null) {
            throw new SaveException(
                    SaveErrorCode.ILLEGAL_GENERATED_ID,
                    path,
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
