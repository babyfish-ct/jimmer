package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.*;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.impl.AbstractExpression;
import org.babyfish.jimmer.sql.ast.impl.Ast;
import org.babyfish.jimmer.sql.ast.impl.ExpressionPrecedences;
import org.babyfish.jimmer.sql.ast.impl.OptimisticLockValueFactoryFactories;
import org.babyfish.jimmer.sql.ast.impl.query.FilterLevel;
import org.babyfish.jimmer.sql.ast.impl.query.MutableRootQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.render.BatchSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.impl.value.PropertyGetter;
import org.babyfish.jimmer.sql.ast.impl.value.ValueGetter;
import org.babyfish.jimmer.sql.ast.mutation.*;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;
import org.babyfish.jimmer.sql.ast.table.spi.UntypedJoinDisabledTableProxy;
import org.babyfish.jimmer.sql.ast.tuple.Tuple3;
import org.babyfish.jimmer.sql.dialect.Dialect;
import org.babyfish.jimmer.sql.exception.ExecutionException;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.IdOnlyFetchType;
import org.babyfish.jimmer.sql.fetcher.impl.FetcherImpl;
import org.babyfish.jimmer.sql.meta.*;
import org.babyfish.jimmer.sql.meta.impl.IdentityIdGenerator;
import org.babyfish.jimmer.sql.meta.impl.SequenceIdGenerator;
import org.babyfish.jimmer.sql.runtime.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.*;
import java.util.*;

class Operator {

    private static final String GENERAL_OPTIMISTIC_DISABLED_JOIN_REASON =
            "Joining is disabled in general optimistic lock";

    private static final int[] SIMPLE_ILLEGAL_ROW_COUNTS = new int[]{-1};

    private static final int[] EMPTY_ROW_COUNTS = new int[0];

    final SaveContext ctx;

    Operator(SaveContext ctx) {
        this.ctx = ctx;
    }

    private static int rowCount(int[] rowCounts) {
        int sumRowCount = 0;
        for (int rowCount : rowCounts) {
            if (rowCount != 0) {
                sumRowCount++;
            }
        }
        return sumRowCount;
    }

    public void insert(Batch<DraftSpi> batch) {

        if (batch.entities().isEmpty() || batch.shape().isIdOnly()) {
            return;
        }
        validate(batch.shape(), true);

        JSqlClientImplementor sqlClient = ctx.options.getSqlClient();
        List<PropertyGetter> defaultGetters = new ArrayList<>();
        for (PropertyGetter getter : Shape.fullOf(sqlClient, batch.shape().getType().getJavaClass()).getGetters()) {
            if (getter.metadata().hasDefaultValue() && !batch.shape().contains(getter)) {
                defaultGetters.add(getter);
            }
        }
        IdentityIdGenerator identityIdGenerator = null;
        SequenceIdGenerator sequenceIdGenerator = null;
        UserIdGenerator<?> userIdGenerator = null;
        if (batch.shape().getIdGetters().isEmpty()) {
            IdGenerator idGenerator = sqlClient.getIdGenerator(ctx.path.getType().getJavaClass());
            if (idGenerator instanceof SequenceIdGenerator) {
                sequenceIdGenerator = (SequenceIdGenerator) idGenerator;
            } else if (idGenerator instanceof UserIdGenerator<?>) {
                userIdGenerator = (UserIdGenerator<?>) idGenerator;
            } else if (idGenerator instanceof IdentityIdGenerator) {
                identityIdGenerator = (IdentityIdGenerator) idGenerator;
            } else {
                ctx.throwIllegalIdGenerator(
                        "In order to insert object without id, the id generator must be identity or sequence"
                );
            }
        }

        if (userIdGenerator != null) {
            Class<?> javaType = ctx.path.getType().getJavaClass();
            PropId idPropId = ctx.path.getType().getIdProp().getId();
            for (DraftSpi draft : batch.entities()) {
                Object id = userIdGenerator.generate(javaType);
                if (id == null || id.getClass() != ctx.path.getType().getIdProp().getReturnClass()) {
                    ctx.throwIllegalGeneratedId(id);
                }
                draft.__set(idPropId, id);
            }
        }

        MetadataStrategy strategy = sqlClient.getMetadataStrategy();
        BatchSqlBuilder builder = new BatchSqlBuilder(
                sqlClient,
                batch.entities().size() < 2 || ctx.options.isBatchForbidden()
        );
        builder.sql("insert into ")
                .sql(ctx.path.getType().getTableName(strategy))
                .enter(BatchSqlBuilder.ScopeType.TUPLE);
        if (sequenceIdGenerator != null) {
            builder.separator().sql(ctx.path.getType().getIdProp().<SingleColumn>getStorage(strategy).getName());
        }

        UpsertMask<?> upsertMask;
        List<ImmutableProp> conflictProps;
        if (batch.originalMode() == SaveMode.UPSERT) {
            upsertMask = ctx.options.getUpsertMask(ctx.path.getType());
            if (!batch.shape().getIdGetters().isEmpty()) {
                conflictProps = Collections.singletonList(batch.shape().getType().getIdProp());
            } else {
                Set<ImmutableProp> keyProps = batch.shape().keyProps(
                        ctx.options.getKeyMatcher(ctx.path.getType())
                );
                conflictProps = new ArrayList<>(keyProps);
                LogicalDeletedInfo logicalDeletedInfo = batch.shape().getType().getLogicalDeletedInfo();
                if (logicalDeletedInfo != null) {
                    conflictProps.add(logicalDeletedInfo.getProp());
                }
            }
        } else {
            upsertMask = null;
            conflictProps = Collections.emptyList();
        }
        for (PropertyGetter getter : batch.shape().getGetters()) {
            if (getter.isInsertable(conflictProps, upsertMask)) {
                builder.separator().sql(getter);
            }
        }
        for (PropertyGetter defaultGetter : defaultGetters) {
            if (defaultGetter.isInsertable(conflictProps, upsertMask)) {
                builder.separator().sql(defaultGetter);
            }
        }
        builder.leave().sql(" values").enter(BatchSqlBuilder.ScopeType.TUPLE);
        if (sequenceIdGenerator != null) {
            builder.separator()
                    .sql("(")
                    .sql(
                            sqlClient.getDialect().getSelectIdFromSequenceSql(sequenceIdGenerator.getSequenceName())
                    )
                    .sql(")");
        } else if (userIdGenerator != null) {
            Shape fullShape = Shape.fullOf(sqlClient, batch.shape().getType().getJavaClass());
            builder.separator();
            for (PropertyGetter getter : fullShape.getIdGetters()) {
                builder.separator().sql(getter);
            }
        }
        for (PropertyGetter getter : batch.shape().getGetters()) {
            if (getter.isInsertable(conflictProps, upsertMask)) {
                builder.separator().variable(getter);
            }
        }
        for (PropertyGetter defaultGetter : defaultGetters) {
            if (defaultGetter.isInsertable(conflictProps, upsertMask)) {
                builder.separator().defaultVariable(defaultGetter);
            }
        }
        builder.leave();
        if ((identityIdGenerator != null || sequenceIdGenerator != null) &&
            sqlClient.getDialect().isInsertedIdReturningRequired()) {
            builder.sql(" returning ")
                    .sql(
                            batch.shape().getType().getIdProp()
                                    .<SingleColumn>getStorage(sqlClient.getMetadataStrategy())
                                    .getName()
                    );
        }

        MutationTrigger trigger = ctx.trigger;
        if (trigger != null) {
            for (DraftSpi draft : batch.entities()) {
                trigger.modifyEntityTable(null, draft);
            }
        }
        int rowCount = execute(builder, batch, false, false);
        AffectedRows.add(ctx.affectedRowCountMap, ctx.path.getType(), rowCount);
    }

    public void update(
            Map<Object, ImmutableSpi> originalIdObjMap,
            Map<KeyMatcher.Group, Map<Object, ImmutableSpi>> originalKeyObjMap,
            Batch<DraftSpi> batch
    ) {
        Shape shape = batch.shape();
        validate(shape, false);
        KeyMatcher.Group group = shape.getIdGetters().isEmpty() ?
                shape.group(ctx.options.getKeyMatcher(shape.getType())) :
                null;
        Set<ImmutableProp> keyProps = group != null ? group.getProps() : null;
        JSqlClientImplementor sqlClient = ctx.options.getSqlClient();
        List<PropertyGetter> idGetters = Shape.fullOf(sqlClient, shape.getType().getJavaClass()).getIdGetters();
        if (group != null && idGetters.size() > 1) {
            throw new IllegalArgumentException(
                    "Cannot update batch whose shape does not have id " +
                    "when id property is embeddable"
            );
        }
        Predicate userOptimisticLockPredicate = userLockOptimisticPredicate();
        PropertyGetter versionGetter = shape.getVersionGetter();
        boolean hasOptimisticLock = userOptimisticLockPredicate != null || versionGetter != null;
        if (hasOptimisticLock && keyProps != null) {
            throw new IllegalArgumentException(
                    "Cannot update batch whose shape does not have id " +
                    "when optimistic lock is required"
            );
        }

        if (batch.entities().isEmpty()) {
            return;
        }

        if (ctx.options.isIdOnlyAsReference(ctx.path.getProp()) &&
                ctx.options.getUnloadedVersionBehavior(shape.getType()) == UnloadedVersionBehavior.IGNORE &&
                shape.isIdOnly()) {
            return;
        }

        Set<ImmutableProp> changedProps =
                originalIdObjMap != null || originalKeyObjMap != null ?
                        new LinkedHashSet<>() :
                        null;
        UpsertMask<?> upsertMask;
        List<ImmutableProp> conflictProps;
        if (batch.originalMode() == SaveMode.UPSERT) {
            upsertMask = ctx.options.getUpsertMask(ctx.path.getType());
            if (!batch.shape().getIdGetters().isEmpty()) {
                conflictProps = Collections.singletonList(batch.shape().getType().getIdProp());
            } else {
                conflictProps = new ArrayList<>(keyProps);
                LogicalDeletedInfo logicalDeletedInfo = batch.shape().getType().getLogicalDeletedInfo();
                if (logicalDeletedInfo != null) {
                    conflictProps.add(logicalDeletedInfo.getProp());
                }
            }
        } else {
            upsertMask = null;
            conflictProps = Collections.emptyList();
        }

        List<PropertyGetter> updatedGetters = new ArrayList<>();
        for (PropertyGetter getter : shape.getGetters()) {
            if (!getter.isUpdatable(conflictProps, upsertMask)) {
                continue;
            }
            ImmutableProp prop = getter.prop();
            if (prop.isId()) {
                continue;
            }
            if (prop.isVersion() && userOptimisticLockPredicate == null) {
                continue;
            }
            if (!prop.isColumnDefinition()) {
                continue;
            }
            if (keyProps != null && keyProps.contains(prop)) {
                continue;
            }
            if (changedProps != null) {
                changedProps.add(prop);
            }
            updatedGetters.add(getter);
        }
        if (updatedGetters.isEmpty() && !hasOptimisticLock) {
            fillIds(QueryReason.GET_ID_WHEN_UPDATE_NOTHING, originalKeyObjMap, batch);
            return;
        }
        if (keyProps != null && !sqlClient.getDialect().isIdFetchableByKeyUpdate()) {
            fillIds(QueryReason.GET_ID_FOR_KEY_BASE_UPDATE, originalKeyObjMap, batch);
            if (batch.entities().isEmpty()) {
                return;
            }
        }
        BatchSqlBuilder builder = new BatchSqlBuilder(
                sqlClient,
                batch.entities().size() < 2 || ctx.options.isBatchForbidden()
        );
        Dialect.UpdateContext updateContext = new UpdateContextImpl(
                builder,
                shape,
                Shape.fullOf(sqlClient, shape.getType().getJavaClass()).getIdGetters().get(0),
                keyProps,
                updatedGetters,
                userOptimisticLockPredicate,
                versionGetter
        );
        sqlClient.getDialect().update(updateContext);

        MutationTrigger trigger = ctx.trigger;
        EntityCollection<DraftSpi> entities = changedProps != null ?
                new EntityList<>(batch.entities().size()) :
                null;
        if (entities != null || trigger != null) {
            if (keyProps != null) {
                Map<Object, ImmutableSpi> subMap = originalIdObjMap != null ?
                        originalKeyObjMap.getOrDefault(group, Collections.emptyMap()) :
                        Collections.emptyMap();
                for (DraftSpi draft : batch.entities()) {
                    ImmutableSpi oldRow = subMap.get(Keys.keyOf(draft, keyProps));
                    if (isChanged(changedProps, oldRow, draft)) {
                        if (trigger != null) {
                            trigger.modifyEntityTable(oldRow, draft);
                        }
                        if (entities != null) {
                            entities.add(draft);
                        }
                    }
                }
            } else {
                PropId idPropId = ctx.path.getType().getIdProp().getId();
                for (DraftSpi draft : batch.entities()) {
                    ImmutableSpi oldRow = originalIdObjMap != null ?
                            originalIdObjMap.get(draft.__get(idPropId)) :
                            null;
                    if (isChanged(changedProps, oldRow, draft) || hasOptimisticLock) {
                        if (trigger != null) {
                            trigger.modifyEntityTable(oldRow, draft);
                        }
                        if (entities != null) {
                            entities.add(draft);
                        }
                    }
                }
            }
        }
        if (entities == null) {
            entities = batch.entities();
        }
        int[] rowCounts = executeAndGetRowCounts(
                builder,
                shape,
                entities,
                true,
                false
        );
        if (versionGetter != null || userOptimisticLockPredicate != null) {
            int index = 0;
            for (DraftSpi row : entities) {
                if (rowCounts[index++] == 0) {
                    ctx.throwOptimisticLockError(row);
                }
            }
        }
        AffectedRows.add(ctx.affectedRowCountMap, ctx.path.getType(), rowCount(rowCounts));
    }

    @SuppressWarnings("unchecked")
    private void fillIds(
            QueryReason queryReason,
            Map<KeyMatcher.Group, Map<Object, ImmutableSpi>> originalKeyObjMap,
            Batch<DraftSpi> batch
    ) {
        KeyMatcher.Group group = batch.shape().group(
                ctx.options.getKeyMatcher(ctx.path.getType())
        );
        Set<ImmutableProp> keyProps = group.getProps();
        Map<KeyMatcher.Group, Map<Object, ImmutableSpi>> keyMap = originalKeyObjMap;
        if (keyMap == null) {
            Fetcher<ImmutableSpi> fetcher = new FetcherImpl<>(
                    (Class<ImmutableSpi>) ctx.path.getType().getJavaClass()
            );
            for (ImmutableProp keyProp : keyProps) {
                if (keyProp.isReference(TargetLevel.ENTITY)) {
                    fetcher = fetcher.add(keyProp.getName(), IdOnlyFetchType.RAW);
                } else {
                    fetcher = fetcher.add(keyProp.getName());
                }
            }
            keyMap = Rows.findMapByKeys(
                    ctx,
                    queryReason,
                    fetcher,
                    batch.entities()
            );
        }
        Map<Object, ImmutableSpi> subMap = keyMap.getOrDefault(group, Collections.emptyMap());
        PropId idPropId = ctx.path.getType().getIdProp().getId();
        for (Iterator<DraftSpi> itr = batch.entities().iterator(); itr.hasNext(); ) {
            DraftSpi draft = itr.next();
            ImmutableSpi row = subMap.get(Keys.keyOf(draft, keyProps));
            if (row != null) {
                draft.__set(idPropId, row.__get(idPropId));
            } else {
                itr.remove();
            }
        }
    }

    public void upsert(Batch<DraftSpi> batch, boolean ignoreUpdate) {

        validate(batch.shape(), false);
        if (batch.entities().isEmpty()) {
            return;
        }
        if (ctx.options.isIdOnlyAsReference(ctx.path.getProp()) && batch.shape().isIdOnly()) {
            return;
        }

        if (ctx.trigger != null) {
            throw new AssertionError(
                    "Internal bug: " +
                    "Upsert cannot be called if the trigger is not null"
            );
        }

        JSqlClientImplementor sqlClient = ctx.options.getSqlClient();
        Shape fullShape = Shape.fullOf(sqlClient, batch.shape().getType().getJavaClass());
        List<PropertyGetter> defaultGetters = new ArrayList<>();
        for (PropertyGetter getter : fullShape.getColumnDefinitionGetters()) {
            if (getter.metadata().hasDefaultValue() && !batch.shape().contains(getter)) {
                defaultGetters.add(getter);
            }
        }
        SequenceIdGenerator sequenceIdGenerator = null;
        if (batch.shape().getIdGetters().isEmpty()) {
            IdGenerator idGenerator = sqlClient.getIdGenerator(ctx.path.getType().getJavaClass());
            if (idGenerator instanceof SequenceIdGenerator) {
                sequenceIdGenerator = (SequenceIdGenerator) idGenerator;
            } else if (!(idGenerator instanceof IdentityIdGenerator)) {
                ctx.throwIllegalIdGenerator(
                        "In order to upsert object without id, " +
                        "the id generator must be IdentityGenerator or Sequence"
                );
            }
        }

        List<ImmutableProp> conflictProps;
        List<PropertyGetter> conflictGetters;
        if (!batch.shape().getIdGetters().isEmpty()) {
            conflictProps = Collections.singletonList(batch.shape().getType().getIdProp());
            conflictGetters = batch.shape().getIdGetters();
        } else {
            Set<ImmutableProp> keyProps = batch.shape().keyProps(
                    ctx.options.getKeyMatcher(ctx.path.getType())
            );
            conflictProps = new ArrayList<>(keyProps);
            LogicalDeletedInfo logicalDeletedInfo = batch.shape().getType().getLogicalDeletedInfo();
            if (logicalDeletedInfo != null) {
                conflictProps.add(logicalDeletedInfo.getProp());
            }
            conflictGetters = new ArrayList<>();
            for (PropertyGetter getter : fullShape.getGetters()) {
                if (keyProps.contains(getter.prop())) {
                    conflictGetters.add(getter);
                } else if (getter.prop().isLogicalDeleted()) {
                    conflictGetters.add(getter);
                }
            }
        }

        UpsertMask<?> upsertMask = ctx.options.getUpsertMask(batch.shape().getType());
        List<PropertyGetter> insertedGetters = new ArrayList<>();
        for (PropertyGetter getter : batch.shape().getColumnDefinitionGetters()) {
            if (getter.isInsertable(conflictProps, upsertMask)) {
                insertedGetters.add(getter);
            }
        }
        for (PropertyGetter getter : defaultGetters) {
            if (getter.isInsertable(conflictProps, upsertMask)) {
                insertedGetters.add(getter);
            }
        }

        List<PropertyGetter> updatedGetters = new ArrayList<>();
        if (!ignoreUpdate) {
            for (PropertyGetter getter : batch.shape().getGetters()) {
                if (getter.isUpdatable(conflictProps, upsertMask)) {
                    updatedGetters.add(getter);
                }
            }
        }

        Predicate userOptimisticLockPredicate = userLockOptimisticPredicate();
        PropertyGetter versionGetter = batch.shape().getVersionGetter();

        BatchSqlBuilder builder = new BatchSqlBuilder(
                sqlClient,
                batch.entities().size() < 2 || ctx.options.isBatchForbidden()
        );
        UpsertContextImpl upsertContext = new UpsertContextImpl(
                builder,
                batch.shape().getIdGetters().isEmpty() ? batch.shape().getType().getIdProp() : null,
                sequenceIdGenerator,
                insertedGetters,
                conflictGetters,
                updatedGetters,
                ignoreUpdate,
                userOptimisticLockPredicate,
                versionGetter
        );
        sqlClient.getDialect().upsert(upsertContext);
        int rowCount = execute(builder, batch, true, ignoreUpdate);
        AffectedRows.add(ctx.affectedRowCountMap, ctx.path.getType(), rowCount);
    }

    private void validate(Shape shape, boolean insertOnly) {
        Set<ImmutableProp> keyProps = shape.keyProps(ctx.options.getKeyMatcher(shape.getType()));
        if (!insertOnly) {
            if (shape.isWild(keyProps)) {
                ctx.throwNeitherIdNorKey(shape.getType(), keyProps);
            }
        }
        MetadataStrategy strategy = ctx.options.getSqlClient().getMetadataStrategy();
        if (!shape.getIdGetters().isEmpty()) {
            ImmutableProp idProp = shape.getType().getIdProp();
            ColumnDefinition definition = shape.getType().getIdProp().getStorage(strategy);
            if (shape.getIdGetters().size() < definition.size()) {
                ctx.throwIncompleteProperty(idProp, "id");
            }
        }
        Map<ImmutableProp, List<PropertyGetter>> getterMap = shape.getGetterMap();
        for (Map.Entry<ImmutableProp, List<PropertyGetter>> e : getterMap.entrySet()) {
            ImmutableProp prop = e.getKey();
            List<PropertyGetter> getter = e.getValue();
            if (prop.isReference(TargetLevel.ENTITY)) {
                ColumnDefinition definition = prop.getStorage(strategy);
                if (getter.size() < definition.size()) {
                    ctx.throwIncompleteProperty(prop, "associated id");
                }
            } else if (keyProps.contains(prop)) {
                ColumnDefinition definition = prop.getStorage(strategy);
                if (getter.size() < definition.size()) {
                    ctx.throwIncompleteProperty(prop, "key");
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Predicate userLockOptimisticPredicate() {

        UserOptimisticLock<Object, Table<Object>> userOptimisticLock =
                (UserOptimisticLock<Object, Table<Object>>) ctx.options.getUserOptimisticLock(ctx.path.getType());
        if (userOptimisticLock == null) {
            return null;
        }
        MutableRootQueryImpl<?> fakeQuery = new MutableRootQueryImpl<>(
                ctx.options.getSqlClient(),
                ctx.path.getType(),
                ExecutionPurpose.MUTATE,
                FilterLevel.DEFAULT
        );
        Table<?> table = fakeQuery.getTable();
        if (table instanceof TableImplementor<?>) {
            table = new UntypedJoinDisabledTableProxy<>(
                    (TableImplementor<?>) table,
                    GENERAL_OPTIMISTIC_DISABLED_JOIN_REASON
            );
        } else {
            table = ((TableProxy<?>) table).__disableJoin(GENERAL_OPTIMISTIC_DISABLED_JOIN_REASON);
        }
        return userOptimisticLock.predicate(
                (Table<Object>) table,
                OptimisticLockValueFactoryFactories.<Object>of()
        );
    }

    private boolean isChanged(Set<ImmutableProp> props, ImmutableSpi oldRow, ImmutableSpi newRow) {
        if (oldRow == null) {
            return true;
        }
        boolean changed = false;
        for (ImmutableProp prop : props) {
            PropId propId = prop.getId();
            boolean isFrozenBackReference = ctx.backReferenceFrozen && prop == ctx.backReferenceProp;
            if (!oldRow.__isLoaded(propId)) {
                if (isFrozenBackReference) {
                    ctx.throwUnloadedFrozenBackReference(ctx.backReferenceProp);
                }
                changed = true;
            } else {
                Object oldValue = oldRow.__get(propId);
                Object newValue = newRow.__get(propId);
                if (isFrozenBackReference && !Objects.equals(oldValue, newValue)) {
                    ctx.throwTargetIsNotTransferable(newRow);
                } else if (!changed && !Objects.equals(oldValue, newValue)) {
                    changed = true;
                }
            }
        }
        return changed;
    }

    private int[] executeAndGetRowCounts(
            BatchSqlBuilder builder,
            Shape shape,
            EntityCollection<DraftSpi> entities,
            boolean updatable,
            boolean ignoreUpdate
    ) {
        if (entities.isEmpty()) {
            return EMPTY_ROW_COUNTS;
        }
        if (entities.size() < 2 || ctx.options.isBatchForbidden() || isForcedOneByOne(shape, entities)) {
            return executeAndGetRowCountsOneByOne(builder, shape, entities, updatable, ignoreUpdate);
        }
        return executeAndGetRowCountsByBatch(builder, shape, entities, updatable, ignoreUpdate);
    }

    private boolean isForcedOneByOne(
            Shape shape,
            EntityCollection<DraftSpi> entities
    ) {
        if (!ctx.options.getSqlClient().getDialect().isBatchDumb()) {
            return false;
        }
        ImmutableProp deepestProp = ctx.path.getProp();
        if (deepestProp != null) {
            if (ctx.options.getAssociatedMode(deepestProp) == AssociatedSaveMode.REPLACE) {
                return true;
            }
            if (deepestProp.isColumnDefinition()) {
                return true;
            }
        }
        List<PropId> postAssociationPropIds = new ArrayList<>();
        for (ImmutableProp prop : shape.getType().getProps().values()) {
            if (!prop.isColumnDefinition() && prop.isAssociation(TargetLevel.ENTITY)) {
                postAssociationPropIds.add(prop.getId());
            }
        }
        if (postAssociationPropIds.isEmpty()) {
            return false;
        }
        PropId idPropId = shape.getType().getIdProp().getId();
        Iterator<EntityCollection.Item<DraftSpi>> itr = entities.items().iterator();
        while (itr.hasNext()) {
            EntityCollection.Item<DraftSpi> item = itr.next();
            if (item.getEntity().__isLoaded(idPropId)) {
                continue;
            }
            for (DraftSpi originalEntity : item.getOriginalEntities()) {
                for (PropId postAssociationPropId : postAssociationPropIds) {
                    if (originalEntity.__isLoaded(postAssociationPropId)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private int[] executeAndGetRowCountsOneByOne(
            BatchSqlBuilder builder,
            Shape shape,
            EntityCollection<DraftSpi> entities,
            boolean updatable,
            boolean ignoreUpdate
    ) {
        JSqlClientImplementor sqlClient = builder.sqlClient();
        Tuple3<String, BatchSqlBuilder.VariableMapper, List<Integer>> tuple = builder.build();
        Executor executor = sqlClient.getExecutor();
        String sql = tuple.get_1();
        BatchSqlBuilder.VariableMapper mapper = tuple.get_2();
        int[] rowCounts = new int[entities.size()];
        int rowIndex = 0;
        ImmutableProp autoIdProp = shape.getIdGetters().isEmpty() ? shape.getType().getIdProp() : null;
        Reader<?> autoIdReader = autoIdProp != null ? sqlClient.getReader(autoIdProp) : null;

        for (EntityCollection.Item<DraftSpi> item : entities.items()) {
            List<Object> variables = mapper.variables(item.getEntity());
            rowCounts[rowIndex++] = executor.execute(
                    new Executor.Args<>(
                            sqlClient,
                            ctx.con,
                            sql,
                            variables,
                            tuple.get_3(),
                            ExecutionPurpose.MUTATE,
                            ctx.options.getExceptionTranslator(),
                            (con, sqlText) -> {
                                if (shape.getIdGetters().isEmpty()) {
                                    return con.prepareStatement(sqlText, Statement.RETURN_GENERATED_KEYS);
                                }
                                return con.prepareStatement(sqlText);
                            },
                            (stmt, args) -> {
                                int rowCount;
                                try {
                                    Savepoint savepoint = SavepointManager.setIfNeeded(ctx.con, sqlClient);
                                    try {
                                        rowCount = stmt.executeUpdate();
                                    } catch (SQLException ex) {
                                        SavepointManager.rollback(stmt::getConnection, savepoint);
                                        throw ex;
                                    } finally {
                                        SavepointManager.release(stmt::getConnection, savepoint);
                                    }
                                } catch (SQLException ex) {
                                    Exception translateException = translateException(ex, args, shape, item.getEntity(), updatable);
                                    if (translateException instanceof RuntimeException) {
                                        throw (RuntimeException) translateException;
                                    }
                                    throw new ExecutionException("Cannot execute the DML statement", translateException);
                                }
                                Object id = null;
                                if (autoIdReader != null) {
                                    try (ResultSet rs = stmt.getGeneratedKeys()) {
                                        if (rs.next()) {
                                            id = autoIdReader.read(rs, new Reader.Context(null, sqlClient));
                                        }
                                    }
                                }
                                modifyEntity(
                                        id,
                                        shape,
                                        item,
                                        updatable,
                                        ignoreUpdate,
                                        rowCount
                                );
                                return rowCount;
                            }
                    )
            );
        }
        return rowCounts;
    }

    private int[] executeAndGetRowCountsByBatch(
            BatchSqlBuilder builder,
            Shape shape,
            EntityCollection<DraftSpi> entities,
            boolean updatable,
            boolean ignoreUpdate
    ) {
        JSqlClientImplementor sqlClient = ctx.options.getSqlClient();
        Tuple3<String, BatchSqlBuilder.VariableMapper, ?> tuple = builder.build();
        try (Executor.BatchContext batchContext = sqlClient
                .getExecutor()
                .executeBatch(
                        ctx.con,
                        tuple.get_1(),
                        shape.getIdGetters().isEmpty() ? ctx.path.getType().getIdProp() : null,
                        ExecutionPurpose.command(QueryReason.NONE),
                        sqlClient
                )
        ) {
            BatchSqlBuilder.VariableMapper mapper = tuple.get_2();
            for (DraftSpi draft : entities) {
                batchContext.add(mapper.variables(draft));
            }
            int[] rowCounts = batchContext.execute((ex, args) -> {
                Executor.BatchContext ctx = (Executor.BatchContext) args;
                if (ex instanceof BatchUpdateException) {
                    modifyEntities(
                            ctx.generatedIds(),
                            shape,
                            entities,
                            updatable,
                            ignoreUpdate,
                            ((BatchUpdateException) ex).getUpdateCounts()
                    );
                }
                return translateException(ex, ctx, shape, entities, updatable);
            });
            if (sqlClient.getDialect().isBatchDumb()) {
                modifyEntities(null, shape, entities, updatable, ignoreUpdate, rowCounts);
            } else {
                modifyEntities(batchContext.generatedIds(), shape, entities, updatable, ignoreUpdate, rowCounts);
            }
            return rowCounts;
        }
    }

    private void modifyEntity(
            Object generatedId,
            Shape shape,
            EntityCollection.Item<DraftSpi> item,
            boolean updatable,
            boolean ignoreUpdate,
            int rowCount
    ) {
        if (generatedId != null) {
            PropId idPropId = ctx.path.getType().getIdProp().getId();
            for (DraftSpi draft : item.getOriginalEntities()) {
                draft.__set(idPropId, generatedId);
            }
        }

        PropertyGetter versionGetter = shape.getVersionGetter();
        if (updatable && versionGetter != null) {
            PropId versionPropId = versionGetter.prop().getId();
            Integer version = (Integer) item.getEntity().__get(versionPropId);
            if (rowCount > 0) {
                for (DraftSpi draft : item.getOriginalEntities()) {
                    draft.__set(versionPropId, version + 1);
                }
            }
        } else if (ignoreUpdate) {
            List<PropId> unloadedPropIds = new ArrayList<>();
            for (ImmutableProp prop : ctx.path.getType().getProps().values()) {
                if (!prop.isMiddleTableDefinition() && prop.isAssociation(TargetLevel.PERSISTENT)) {
                    unloadedPropIds.add(prop.getId());
                }
            }
            if (unloadedPropIds.isEmpty()) {
                return;
            }
            if (rowCount <= 0) {
                for (PropId unloadedPropId : unloadedPropIds) {
                    for (DraftSpi draft : item.getOriginalEntities()) {
                        draft.__unload(unloadedPropId);
                    }
                }
            }
        }
    }

    private void modifyEntities(
            Object[] generatedIds,
            Shape shape,
            EntityCollection<DraftSpi> entities,
            boolean updatable,
            boolean ignoreUpdate,
            int[] rowCounts
    ) {
        int generateIdIndex = 0;
        int rowIndex = 0;
        for (EntityCollection.Item<DraftSpi> item : entities.items()) {
            modifyEntity(
                    generatedIds != null && generatedIds.length != 0 && rowCounts[rowIndex] > 0 ?
                            generatedIds[generateIdIndex++] :
                            null,
                    shape,
                    item,
                    updatable,
                    ignoreUpdate,
                    rowIndex < rowCounts.length ?
                            rowCounts[rowIndex] :
                            -1
            );
            rowIndex++;
        }
    }

    private int execute(
            BatchSqlBuilder builder,
            Batch<DraftSpi> batch,
            boolean updatable,
            boolean ignoreUpdate
    ) {
        int[] rowCounts = executeAndGetRowCounts(
                builder,
                batch.shape(),
                batch.entities(),
                updatable,
                ignoreUpdate
        );
        return rowCount(rowCounts);
    }

    private Exception translateException(
            SQLException ex,
            Executor.Args<?> args,
            Shape shape,
            DraftSpi entity,
            boolean updatable
    ) {
        String state = ex.getSQLState();
        if (state == null || !state.startsWith("23")) {
            return convertFinalException(ex, args);
        }
        EntityInvestigator investigator = new EntityInvestigator(
                SIMPLE_ILLEGAL_ROW_COUNTS,
                this.ctx.investigator(ctx.options.getSqlClient()),
                shape,
                Collections.singletonList(entity),
                updatable
        );
        Exception investigateEx = investigator.investigate();
        if (investigateEx == null) {
            investigateEx = ex;
        }
        return convertFinalException(investigateEx, args);
    }

    private Exception translateException(
            SQLException ex,
            @Nullable Executor.BatchContext ctx,
            Shape shape,
            Collection<? extends DraftSpi> entities,
            boolean updatable
    ) {
        String state = ex.getSQLState();
        if (state == null || !state.startsWith("23") || !(ex instanceof BatchUpdateException)) {
            return convertFinalException(ex, ctx);
        }
        BatchUpdateException bue = (BatchUpdateException) ex;
        EntityInvestigator investigator = new EntityInvestigator(
                bue.getUpdateCounts(),
                this.ctx.investigator(ctx),
                shape,
                entities,
                updatable
        );
        Exception investigateEx = investigator.investigate();
        if (investigateEx == null) {
            investigateEx = bue;
        }
        return convertFinalException(investigateEx, ctx);
    }

    private Exception convertFinalException(@NotNull Exception ex, @NotNull ExceptionTranslator.Args args) {
        ExceptionTranslator<Exception> translator =
                this.ctx.options.getExceptionTranslator();
        if (translator == null) {
            return ex;
        }
        return translator.translate(ex, args);
    }

    private class UpdateContextImpl implements Dialect.UpdateContext {

        private final BatchSqlBuilder builder;

        private final Shape shape;

        private final PropertyGetter idGetter;

        private final Set<ImmutableProp> keyProps;

        private final List<PropertyGetter> updatedGetters;

        private final Predicate userOptimisticLockPredicate;

        private final PropertyGetter versionGetter;

        UpdateContextImpl(
                BatchSqlBuilder builder,
                Shape shape,
                PropertyGetter idGetter,
                Set<ImmutableProp> keyProps,
                List<PropertyGetter> updatedGetters,
                Predicate userOptimisticLockPredicate,
                PropertyGetter versionGetter
        ) {
            this.builder = builder;
            this.shape = shape;
            this.idGetter = idGetter;
            this.keyProps = keyProps;
            this.updatedGetters = updatedGetters;
            this.userOptimisticLockPredicate = userOptimisticLockPredicate;
            this.versionGetter = versionGetter;
        }

        @Override
        public boolean isUpdatedByKey() {
            return shape.getIdGetters().isEmpty();
        }

        @Override
        public Dialect.UpdateContext sql(String sql) {
            builder.sql(sql);
            return this;
        }

        @Override
        public Dialect.UpdateContext sql(ValueGetter getter) {
            builder.sql(getter);
            return this;
        }

        @Override
        public Dialect.UpdateContext enter(AbstractSqlBuilder.ScopeType type) {
            builder.enter(type);
            return this;
        }

        @Override
        public Dialect.UpdateContext separator() {
            builder.separator();
            return this;
        }

        @Override
        public Dialect.UpdateContext leave() {
            builder.leave();
            return this;
        }

        @Override
        public Dialect.UpdateContext appendTableName() {
            MetadataStrategy strategy = ctx.options.getSqlClient().getMetadataStrategy();
            builder.sql(ctx.path.getType().getTableName(strategy));
            return this;
        }

        @Override
        public Dialect.UpdateContext appendAssignments() {
            for (PropertyGetter getter : updatedGetters) {
                if (getter != versionGetter) {
                    builder.separator()
                            .sql(getter)
                            .sql(" = ")
                            .variable(getter);
                }
            }

            PropertyGetter actualVersionGetter = versionGetter;
            if (actualVersionGetter == null) {
                ImmutableProp versionProp = ctx.path.getType().getVersionProp();
                if (versionProp != null &&
                    ctx.options.getUnloadedVersionBehavior(ctx.path.getType()) == UnloadedVersionBehavior.INCREASE
                ) {
                    actualVersionGetter = PropertyGetter
                            .propertyGetters(ctx.options.getSqlClient(), versionProp)
                            .get(0);
                }
            }
            if (actualVersionGetter != null) {
                builder.separator()
                        .sql(actualVersionGetter)
                        .sql(" = ")
                        .sql(actualVersionGetter)
                        .sql(" + 1");
            }
            return this;
        }

        @Override
        public Dialect.UpdateContext appendPredicates() {
            if (keyProps != null) {
                Map<ImmutableProp, List<PropertyGetter>> getterMap = shape.getGetterMap();
                for (ImmutableProp keyProp : keyProps) {
                    List<PropertyGetter> getters = getterMap.get(keyProp);
                    for (PropertyGetter getter : getters) {
                        builder.separator()
                                .sql(getter)
                                .sql(" = ")
                                .variable(getter);
                    }
                }
            } else {
                for (PropertyGetter getter : shape.getIdGetters()) {
                    builder.separator()
                            .sql(getter)
                            .sql(" = ")
                            .variable(getter);
                }
            }
            if (versionGetter != null) {
                builder.separator()
                        .sql(versionGetter)
                        .sql(" = ")
                        .variable(versionGetter);
            }
            if (userOptimisticLockPredicate != null) {
                builder.separator();
                AbstractExpression.renderChild(
                        (Ast) userOptimisticLockPredicate,
                        ExpressionPrecedences.AND,
                        builder
                );
            }
            return this;
        }

        @Override
        public Dialect.UpdateContext appendId() {
            builder.sql(idGetter);
            return this;
        }
    }

    private class UpsertContextImpl implements Dialect.UpsertContext {

        private final BatchSqlBuilder builder;

        private final SequenceIdGenerator sequenceIdGenerator;

        private final PropertyGetter generatedIdGetter;

        private final List<PropertyGetter> insertedGetters;

        private final List<PropertyGetter> conflictGetters;

        private final List<PropertyGetter> updatedGetters;

        private final boolean updateIgnored;

        private final Predicate userOptimisticLockPredicate;

        private final PropertyGetter versionGetter;

        private Boolean complete;

        UpsertContextImpl(
                BatchSqlBuilder builder,
                ImmutableProp generatedIdProp,
                SequenceIdGenerator sequenceIdGenerator,
                List<PropertyGetter> insertedGetters,
                List<PropertyGetter> conflictGetters,
                List<PropertyGetter> updatedGetters,
                boolean updateIgnored,
                Predicate userOptimisticLockPredicate,
                PropertyGetter versionGetter
        ) {
            if (generatedIdProp != null && generatedIdProp.isEmbedded(EmbeddedLevel.SCALAR)) {
                throw new IllegalArgumentException("Generated id prop cannot be embeddable");
            }
            if (generatedIdProp != null && (
                    userOptimisticLockPredicate != null || versionGetter != null)
            ) {
                throw new IllegalArgumentException(
                        "Optimistic lock is not support by upsert statement which can generate id"
                );
            }
            this.builder = builder;
            this.sequenceIdGenerator = sequenceIdGenerator;
            this.generatedIdGetter = generatedIdProp != null ?
                    Shape.fullOf(builder.sqlClient(), generatedIdProp.getDeclaringType().getJavaClass())
                            .getIdGetters()
                            .get(0) :
                    null;
            this.insertedGetters = insertedGetters;
            this.conflictGetters = conflictGetters;
            this.updatedGetters = updatedGetters;
            this.updateIgnored = updateIgnored;
            this.userOptimisticLockPredicate = userOptimisticLockPredicate;
            this.versionGetter = versionGetter;
        }

        @Override
        public boolean hasUpdatedColumns() {
            return !updatedGetters.isEmpty();
        }

        @Override
        public boolean hasOptimisticLock() {
            return userOptimisticLockPredicate != null || versionGetter != null;
        }

        @Override
        public boolean hasGeneratedId() {
            return generatedIdGetter != null;
        }

        @Override
        public boolean isUpdateIgnored() {
            return updateIgnored;
        }

        @Override
        public boolean isComplete() {
            Boolean complete = this.complete;
            if (complete == null) {
                this.complete = complete = isComplete0();
            }
            return complete;
        }

        private boolean isComplete0() {
            for (PropertyGetter getter : insertedGetters) {
                if (!conflictGetters.contains(getter) && !updatedGetters.contains(getter)) {
                    return false;
                }
            }
            if (updatedGetters.isEmpty()) {
                return false;
            }
            for (PropertyGetter getter : updatedGetters) {
                if (!insertedGetters.contains(getter)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public List<ValueGetter> getConflictGetters() {
            return Collections.unmodifiableList(conflictGetters);
        }

        @Override
        public Dialect.UpsertContext sql(String sql) {
            builder.sql(sql);
            return this;
        }

        @Override
        public Dialect.UpsertContext sql(ValueGetter getter) {
            builder.sql(getter);
            return this;
        }

        @Override
        public Dialect.UpsertContext enter(AbstractSqlBuilder.ScopeType type) {
            builder.enter(type);
            return this;
        }

        @Override
        public Dialect.UpsertContext separator() {
            builder.separator();
            return this;
        }

        @Override
        public Dialect.UpsertContext leave() {
            builder.leave();
            return this;
        }

        @Override
        public Dialect.UpsertContext appendTableName() {
            builder.sql(ctx.path.getType().getTableName(ctx.options.getSqlClient().getMetadataStrategy()));
            return this;
        }

        @Override
        public Dialect.UpsertContext appendInsertedColumns(String prefix) {
            if (sequenceIdGenerator != null) {
                builder.separator()
                        .sql("(")
                        .sql(
                                builder.sqlClient()
                                        .getDialect()
                                        .getSelectIdFromSequenceSql(sequenceIdGenerator.getSequenceName())
                        )
                        .sql(")");
            }
            for (PropertyGetter getter : insertedGetters) {
                if (!getter.prop().isId() || sequenceIdGenerator == null) {
                    builder.separator().sql(prefix).sql(getter);
                }
            }
            return this;
        }

        @Override
        public Dialect.UpsertContext appendConflictColumns() {
            for (PropertyGetter getter : conflictGetters) {
                builder.separator().sql(getter);
            }
            return this;
        }

        @Override
        public Dialect.UpsertContext appendInsertingValues() {
            builder.enter(BatchSqlBuilder.ScopeType.COMMA);
            for (PropertyGetter getter : insertedGetters) {
                builder.separator().variable(getter);
            }
            builder.leave();
            return this;
        }

        @Override
        public Dialect.UpsertContext appendUpdatingAssignments(String prefix, String suffix) {
            for (PropertyGetter getter : updatedGetters) {
                builder.separator()
                        .sql(getter)
                        .sql(" = ");
                if (getter.metadata().getValueProp().isVersion() && ctx.options.getUserOptimisticLock(ctx.path.getType()) == null) {
                    builder.sql(prefix)
                            .sql(getter)
                            .sql(" + 1");
                } else {
                    builder.sql(prefix)
                            .sql(getter)
                            .sql(suffix);
                }
            }
            return this;
        }

        @Override
        public Dialect.UpsertContext appendOptimisticLockCondition(String sourceTablePrefix) {
            if (userOptimisticLockPredicate != null) {
                ((Ast) userOptimisticLockPredicate).renderTo(builder);
            }
            if (versionGetter != null) {
                builder
                        .sql(ctx.path.getType().getTableName(ctx.options.getSqlClient().getMetadataStrategy()))
                        .sql(".")
                        .sql(versionGetter)
                        .sql(" = ")
                        .sql(sourceTablePrefix)
                        .sql(versionGetter);
            }
            return this;
        }

        @Override
        public Dialect.UpsertContext appendGeneratedId() {
            if (generatedIdGetter != null) {
                builder.sql(generatedIdGetter);
            }
            return this;
        }
    }
}
