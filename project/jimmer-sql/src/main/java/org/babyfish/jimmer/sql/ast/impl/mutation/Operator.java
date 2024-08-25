package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.EmbeddedLevel;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.PropId;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.impl.Ast;
import org.babyfish.jimmer.sql.ast.impl.OptimisticLockValueFactoryFactories;
import org.babyfish.jimmer.sql.ast.impl.query.FilterLevel;
import org.babyfish.jimmer.sql.ast.impl.query.MutableRootQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.render.BatchSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.impl.value.PropertyGetter;
import org.babyfish.jimmer.sql.ast.impl.value.ValueGetter;
import org.babyfish.jimmer.sql.ast.mutation.UserOptimisticLock;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;
import org.babyfish.jimmer.sql.ast.table.spi.UntypedJoinDisabledTableProxy;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.dialect.Dialect;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.IdOnlyFetchType;
import org.babyfish.jimmer.sql.fetcher.impl.FetcherImpl;
import org.babyfish.jimmer.sql.meta.IdGenerator;
import org.babyfish.jimmer.sql.meta.MetadataStrategy;
import org.babyfish.jimmer.sql.meta.SingleColumn;
import org.babyfish.jimmer.sql.meta.UserIdGenerator;
import org.babyfish.jimmer.sql.meta.impl.IdentityIdGenerator;
import org.babyfish.jimmer.sql.meta.impl.SequenceIdGenerator;
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose;
import org.babyfish.jimmer.sql.runtime.Executor;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.babyfish.jimmer.sql.runtime.SaveException;
import org.jetbrains.annotations.Nullable;

import java.util.*;

class Operator {

    private static final String GENERAL_OPTIMISTIC_DISABLED_JOIN_REASON =
            "Joining is disabled in general optimistic lock";

    private static final int[] EMPTY_ROW_COUNTS = new int[0];

    final SaveContext ctx;

    Operator(SaveContext ctx) {
        this.ctx = ctx;
    }

    public void insert(Batch<DraftSpi> batch) {

        if (batch.entities().isEmpty() || batch.shape().isIdOnly()) {
            return;
        }

        JSqlClientImplementor sqlClient = ctx.options.getSqlClient();

        List<PropertyGetter> defaultGetters = new ArrayList<>();
        for (PropertyGetter getter : Shape.fullOf(sqlClient, batch.shape().getType().getJavaClass()).getGetters()) {
            if (getter.metadata().hasDefaultValue() && !batch.shape().contains(getter)) {
                defaultGetters.add(getter);
            }
        }
        SequenceIdGenerator sequenceIdGenerator = null;
        UserIdGenerator<?> userIdGenerator = null;
        if (batch.shape().getIdGetters().isEmpty()) {
            IdGenerator idGenerator = sqlClient.getIdGenerator(ctx.path.getType().getJavaClass());
            if (idGenerator instanceof SequenceIdGenerator) {
                sequenceIdGenerator = (SequenceIdGenerator) idGenerator;
            } else if (idGenerator instanceof UserIdGenerator<?>) {
                userIdGenerator = (UserIdGenerator<?>) idGenerator;
            } else if (!(idGenerator instanceof IdentityIdGenerator)) {
                throw new SaveException.IllegalIdGenerator(
                        ctx.path,
                        "In order to insert object without id, the id generator must be identity or sequence"
                );
            }
        }

        if (userIdGenerator != null) {
            Class<?> javaType = ctx.path.getType().getJavaClass();
            PropId idPropId = ctx.path.getType().getIdProp().getId();
            for (DraftSpi draft : batch.entities()) {
                draft.__set(idPropId, userIdGenerator.generate(javaType));
            }
        }

        MetadataStrategy strategy = sqlClient.getMetadataStrategy();
        BatchSqlBuilder builder = new BatchSqlBuilder(sqlClient);
        builder.sql("insert into ")
                .sql(ctx.path.getType().getTableName(strategy))
                .enter(BatchSqlBuilder.ScopeType.TUPLE);
        if (sequenceIdGenerator != null) {
            builder.separator().sql(ctx.path.getType().getIdProp().<SingleColumn>getStorage(strategy).getName());
        }
        for (PropertyGetter getter : batch.shape().getGetters()) {
            builder.separator().sql(getter);
        }
        for (PropertyGetter defaultGetter : defaultGetters) {
            builder.separator().sql(defaultGetter);
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
            builder.separator().variable(getter);
        }
        for (PropertyGetter defaultGetter : defaultGetters) {
            builder.separator().defaultVariable(defaultGetter);
        }
        builder.leave();

        MutationTrigger trigger = ctx.trigger;
        if (trigger != null) {
            for (DraftSpi draft : batch.entities()) {
                trigger.modifyEntityTable(null, draft);
            }
        }
        int rowCount = execute(builder, batch, false);
        AffectedRows.add(ctx.affectedRowCountMap, ctx.path.getType(), rowCount);
    }

    public void update(
            Map<Object, ImmutableSpi> originalIdObjMap,
            Map<Object, ImmutableSpi> originalKeyObjMap,
            Batch<DraftSpi> batch
    ) {
        Set<ImmutableProp> keyProps = batch.shape().getIdGetters().isEmpty() ?
                ctx.options.getKeyProps(batch.shape().getType()) :
                null;
        Predicate userOptimisticLockPredicate = userLockOptimisticPredicate();
        PropertyGetter versionGetter = batch.shape().getVersionGetter();
        boolean updateVersion = userOptimisticLockPredicate == null && versionGetter != null;
        if (updateVersion && keyProps != null) {
            throw new IllegalArgumentException(
                    "Cannot update batch whose shape does not have id " +
                            "when optimistic lock is required"
            );
        }
        if (batch.entities().isEmpty() || batch.shape().isIdOnly()) {
            return;
        }

        JSqlClientImplementor sqlClient = ctx.options.getSqlClient();

        Set<ImmutableProp> changedProps =
                originalIdObjMap != null || originalKeyObjMap != null ?
                        new LinkedHashSet<>() :
                        null;
        List<PropertyGetter> updatedGetters = new ArrayList<>();
        for (PropertyGetter getter : batch.shape().getGetters()) {
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
        if (updatedGetters.isEmpty() && !updateVersion) {
            fillIds(QueryReason.GET_ID_WHEN_UPDATE_NOTHING, originalKeyObjMap, batch);
            return;
        }
        if (keyProps != null && !sqlClient.getDialect().isUpdateByKySupported()) {
            fillIds(QueryReason.GET_ID_FOR_KEY_BASE_UPDATE, originalKeyObjMap, batch);
            if (batch.entities().isEmpty()) {
                return;
            }
        }
        BatchSqlBuilder builder = new BatchSqlBuilder(sqlClient);
        Dialect.UpdateContext updateContext = new UpdateContextImpl(
                builder,
                batch.shape(),
                Shape.fullOf(sqlClient, batch.shape().getType().getJavaClass()).getIdGetters().get(0),
                keyProps,
                updatedGetters,
                updateVersion,
                userOptimisticLockPredicate,
                versionGetter
        );
        sqlClient.getDialect().update(updateContext);

        MutationTrigger trigger = ctx.trigger;
        Collection<DraftSpi> entities = changedProps != null ?
                new ArrayList<>(batch.entities().size()) :
                null;
        if (entities != null || trigger != null) {
            if (keyProps != null) {
                for (DraftSpi draft : batch.entities()) {
                    ImmutableSpi oldRow = originalKeyObjMap != null ?
                            originalKeyObjMap.get(Keys.keyOf(draft, keyProps)) :
                            null;
                    if (isChanged(changedProps, oldRow, draft) || updateVersion) {
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
                    if (isChanged(changedProps, oldRow, draft) || updateVersion) {
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
                batch.shape(),
                entities,
                true
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
            Map<Object, ImmutableSpi> originalKeyObjMap,
            Batch<DraftSpi> batch
    ) {
        Set<ImmutableProp> keyProps = ctx.options.getKeyProps(ctx.path.getType());
        Map<Object, ImmutableSpi> keyMap = originalKeyObjMap;
        if (keyMap == null) {
            Fetcher<ImmutableSpi> fetcher = new FetcherImpl<>(
                    (Class<ImmutableSpi>)ctx.path.getType().getJavaClass()
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
        PropId idPropId = ctx.path.getType().getIdProp().getId();
        for (Iterator<DraftSpi> itr = batch.entities().iterator(); itr.hasNext(); ) {
            DraftSpi draft = itr.next();
            ImmutableSpi row = keyMap.get(Keys.keyOf(draft, keyProps));
            if (row != null) {
                draft.__set(idPropId, row.__get(idPropId));
            } else {
                itr.remove();
            }
        }
    }

    public void upsert(Batch<DraftSpi> batch) {
        if (batch.entities().isEmpty() || batch.shape().isIdOnly()) {
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
                throw new SaveException.IllegalIdGenerator(
                        ctx.path,
                        "In order to upsert object without id, " +
                                "the id generator must be IdentityGenerator or Sequence"
                );
            }
        }

        List<PropertyGetter> insertedGetters = new ArrayList<>();
        insertedGetters.addAll(batch.shape().getColumnDefinitionGetters());
        insertedGetters.addAll(defaultGetters);

        List<PropertyGetter> conflictGetters = new ArrayList<>();
        if (!batch.shape().getIdGetters().isEmpty()) {
            conflictGetters.addAll(batch.shape().getIdGetters());
        } else {
            Set<ImmutableProp> keyProps = ctx.options.getKeyProps(ctx.path.getType());
            for (PropertyGetter getter : fullShape.getGetters()) {
                if (keyProps.contains(getter.prop())) {
                    conflictGetters.add(getter);
                }
            }
        }

        List<PropertyGetter> updatedGetters = new ArrayList<>();
        for (PropertyGetter getter : batch.shape().getGetters()) {
            if (!conflictGetters.contains(getter)) {
                updatedGetters.add(getter);
            }
        }
        for (PropertyGetter defaultGetter : defaultGetters) {
            if (!conflictGetters.contains(defaultGetter)) {
                updatedGetters.add(defaultGetter);
            }
        }

        Predicate userOptimisticLockPredicate = userLockOptimisticPredicate();
        PropertyGetter versionGetter = batch.shape().getVersionGetter();

        BatchSqlBuilder builder = new BatchSqlBuilder(sqlClient);
        UpsertContextImpl upsertContext = new UpsertContextImpl(
                builder,
                batch.shape().getIdGetters().isEmpty() ? batch.shape().getType().getIdProp() : null,
                sequenceIdGenerator,
                insertedGetters,
                conflictGetters,
                updatedGetters,
                userOptimisticLockPredicate,
                versionGetter
        );
        sqlClient.getDialect().upsert(upsertContext);
        int rowCount = execute(builder, batch, true);
        AffectedRows.add(ctx.affectedRowCountMap, ctx.path.getType(), rowCount);
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
            table = ((TableProxy<?>)table).__disableJoin(GENERAL_OPTIMISTIC_DISABLED_JOIN_REASON);
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
        for (ImmutableProp prop : props) {
            PropId propId = prop.getId();
            if (!oldRow.__isLoaded(propId)) {
                return true;
            }
            Object oldValue = oldRow.__get(propId);
            Object newValue = newRow.__get(propId);
            if (!oldRow.__isLoaded(propId) || !Objects.equals(oldValue, newValue)) {
                if (ctx.backReferenceFrozen && prop == ctx.backReferenceProp && oldValue != null && newValue != null) {
                    ctx.throwTargetIsNotTransferable(newRow);
                }
                return true;
            }
        }
        return false;
    }

    private int[] executeAndGetRowCounts(
            BatchSqlBuilder builder,
            Shape shape,
            Collection<DraftSpi> entities,
            boolean updatable
    ) {
        if (entities.isEmpty()) {
            return EMPTY_ROW_COUNTS;
        }
        JSqlClientImplementor sqlClient = ctx.options.getSqlClient();
        PropertyGetter versionGetter = shape.getVersionGetter();
        Tuple2<String, BatchSqlBuilder.VariableMapper> tuple = builder.build();
        try (Executor.BatchContext batchContext = sqlClient
                .getExecutor()
                .executeBatch(
                        sqlClient,
                        ctx.con,
                        tuple.get_1(),
                        shape.getIdGetters().isEmpty() ? ctx.path.getType().getIdProp() : null
                )
        ) {
            BatchSqlBuilder.VariableMapper mapper = tuple.get_2();
            for (DraftSpi draft : entities) {
                batchContext.add(mapper.variables(draft));
            }
            int[] rowCounts = batchContext.execute();

            if (shape.getIdGetters().isEmpty()) {
                Object[] generatedIds = batchContext.generatedIds();
                if (generatedIds.length != entities.size()) {
                    throw new IllegalStateException(
                            "The inserted row count is " +
                                    entities.size() +
                                    ", but the count of generated ids is " +
                                    generatedIds.length
                    );
                }
                PropId idPropId = ctx.path.getType().getIdProp().getId();
                int index = 0;
                int generatedIndex = 0;
                for (DraftSpi draft : entities) {
                    if (rowCounts[index++] != 0) {
                        Object id = generatedIds[generatedIndex++];
                        if (id != null) {
                            draft.__set(idPropId, id);
                        }
                    }
                }
            }

            if (updatable && versionGetter != null) {
                PropId versionPropId = versionGetter.prop().getId();
                Iterator<DraftSpi> itr = entities.iterator();
                for (int rowCount : rowCounts) {
                    DraftSpi draft = itr.next();
                    if (rowCount == 0) {
                        ctx.throwOptimisticLockError(draft);
                    }
                    Integer version = (Integer) draft.__get(versionPropId);
                    draft.__set(versionPropId, version + 1);
                }
            }
            return rowCounts;
        }
    }

    private int execute(
            BatchSqlBuilder builder,
            Batch<DraftSpi> batch,
            boolean updatable
    ) {
        int[] rowCounts = executeAndGetRowCounts(builder, batch.shape(), batch.entities(), updatable);
        return rowCount(rowCounts);
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

    private class UpdateContextImpl implements Dialect.UpdateContext {

        private final BatchSqlBuilder builder;

        private final Shape shape;

        private final PropertyGetter idGetter;

        private final Set<ImmutableProp> keyProps;

        private final List<PropertyGetter> updatedGetters;

        private final boolean updateVersion;

        private final Predicate userOptimisticLockPredicate;

        private final PropertyGetter versionGetter;

        private UpdateContextImpl(
                BatchSqlBuilder builder,
                Shape shape,
                PropertyGetter idGetter,
                Set<ImmutableProp> keyProps,
                List<PropertyGetter> updatedGetters,
                boolean updateVersion,
                Predicate userOptimisticLockPredicate,
                PropertyGetter versionGetter
        ) {
            this.builder = builder;
            this.shape = shape;
            this.idGetter = idGetter;
            this.keyProps = keyProps;
            this.updatedGetters = updatedGetters;
            this.updateVersion = updateVersion;
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
                builder.separator()
                        .sql(getter)
                        .sql(" = ")
                        .variable(getter);
            }
            if (updateVersion) {
                builder.separator()
                        .sql(versionGetter)
                        .sql(" = ")
                        .sql(versionGetter)
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
            if (userOptimisticLockPredicate != null) {
                builder.separator();
                ((Ast)userOptimisticLockPredicate).renderTo(builder);
            } else if (versionGetter != null) {
                builder.separator()
                        .sql(versionGetter)
                        .sql(" = ")
                        .variable(versionGetter);
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

        private final Predicate userOptimisticLockPredicate;

        private final PropertyGetter versionGetter;

        private UpsertContextImpl(
                BatchSqlBuilder builder,
                ImmutableProp generatedIdProp,
                SequenceIdGenerator sequenceIdGenerator,
                List<PropertyGetter> insertedGetters,
                List<PropertyGetter> conflictGetters,
                List<PropertyGetter> updatedGetters,
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
        public Dialect.UpsertContext appendTableName() {
            builder.sql(ctx.path.getType().getTableName(ctx.options.getSqlClient().getMetadataStrategy()));
            return this;
        }

        @Override
        public Dialect.UpsertContext appendInsertedColumns() {
            builder.enter(BatchSqlBuilder.ScopeType.COMMA);
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
                    builder.separator().sql(getter);
                }
            }
            builder.leave();
            return this;
        }

        @Override
        public Dialect.UpsertContext appendConflictColumns() {
            builder.enter(AbstractSqlBuilder.ScopeType.COMMA);
            for (PropertyGetter getter : conflictGetters) {
                builder.separator().sql(getter);
            }
            builder.leave();
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
            builder.enter(BatchSqlBuilder.ScopeType.COMMA);
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
            builder.leave();
            return this;
        }

        @Override
        public Dialect.UpsertContext appendOptimisticLockCondition() {
            if (userOptimisticLockPredicate != null) {
                ((Ast)userOptimisticLockPredicate).renderTo(builder);
            } if (versionGetter != null) {
                builder.sql(versionGetter)
                        .sql(" = ")
                        .variable(versionGetter);
            }
            return this;
        }

        @Nullable
        @Override
        public PropertyGetter getGeneratedIdGetter() {
            return generatedIdGetter;
        }

        @Override
        public List<ValueGetter> getConflictGetters() {
            return Collections.unmodifiableList(conflictGetters);
        }
    }
}
