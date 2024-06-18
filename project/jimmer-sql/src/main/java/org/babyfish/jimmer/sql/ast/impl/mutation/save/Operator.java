package org.babyfish.jimmer.sql.ast.impl.mutation.save;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.PropId;
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
import org.babyfish.jimmer.sql.ast.mutation.UserOptimisticLock;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;
import org.babyfish.jimmer.sql.ast.table.spi.UntypedJoinDisabledTableProxy;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.dialect.Dialect;
import org.babyfish.jimmer.sql.meta.IdGenerator;
import org.babyfish.jimmer.sql.meta.MetadataStrategy;
import org.babyfish.jimmer.sql.meta.SingleColumn;
import org.babyfish.jimmer.sql.meta.impl.IdentityIdGenerator;
import org.babyfish.jimmer.sql.meta.impl.SequenceIdGenerator;
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose;
import org.babyfish.jimmer.sql.runtime.Executor;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.babyfish.jimmer.sql.runtime.SaveException;

import java.util.*;

class Operator {

    private static final String GENERAL_OPTIMISTIC_DISABLED_JOIN_REASON =
            "Joining is disabled in general optimistic lock";

    final SaveContext ctx;

    Operator(SaveContext ctx) {
        this.ctx = ctx;
    }

    public int insert(Batch<DraftSpi> batch) {

        if (batch.entities().isEmpty()) {
            return 0;
        }

        JSqlClientImplementor sqlClient = ctx.options.getSqlClient();

        List<PropertyGetter> defaultGetters = new ArrayList<>();
        for (PropertyGetter getter : Shape.fullOf(sqlClient, batch.shape().getType().getJavaClass()).getGetters()) {
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
                        "In order to insert object without id, the id generator must be identity or sequence"
                );
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
            builder.separator().sql(getter.columnName());
        }
        for (PropertyGetter defaultGetter : defaultGetters) {
            builder.separator().sql(defaultGetter.columnName());
        }
        builder.leave().sql(" values").enter(BatchSqlBuilder.ScopeType.TUPLE);
        if (sequenceIdGenerator != null) {
            builder.separator()
                    .sql("(")
                    .sql(
                            sqlClient.getDialect().getSelectIdFromSequenceSql(sequenceIdGenerator.getSequenceName())
                    )
                    .sql(")");
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
        return execute(builder, batch, false);
    }

    public int update(
            Map<Object, ImmutableSpi> originalIdObjMap,
            Map<Object, ImmutableSpi> originalKeyObjMap,
            Batch<DraftSpi> batch
    ) {

        if (batch.shape().getIdGetters().isEmpty()) {
            throw new IllegalArgumentException("Cannot update batch whose shape does not have id");
        }
        if (batch.entities().isEmpty()) {
            return 0;
        }

        JSqlClientImplementor sqlClient = ctx.options.getSqlClient();
        MetadataStrategy strategy = sqlClient.getMetadataStrategy();
        Predicate userOptimisticLockPredicate = userLockOptimisticPredicate();
        PropertyGetter versionGetter = batch.shape().getVersionGetter();
        if (userOptimisticLockPredicate == null && versionGetter == null && ctx.path.getType().getVersionProp() != null) {
            ctx.throwNoVersionError();
        }

        BatchSqlBuilder builder = new BatchSqlBuilder(sqlClient);
        builder.sql("update ")
                .sql(ctx.path.getType().getTableName(strategy))
                .enter(BatchSqlBuilder.ScopeType.SET);
        for (PropertyGetter getter : batch.shape().getGetters()) {
            if (getter.prop().isId()) {
                continue;
            }
            if (getter.prop().isVersion() && userOptimisticLockPredicate == null) {
                continue;
            }
            builder.separator()
                    .sql(getter.columnName())
                    .sql(" = ")
                    .variable(getter);
        }
        if (userOptimisticLockPredicate == null && versionGetter != null) {
            builder.separator()
                    .sql(versionGetter.columnName())
                    .sql(" = ")
                    .sql(versionGetter.columnName())
                    .sql(" + 1");
        }
        builder.leave().enter(BatchSqlBuilder.ScopeType.WHERE);
        for (PropertyGetter getter : batch.shape().getIdGetters()) {
            builder.separator()
                    .sql(getter.columnName())
                    .sql(" = ")
                    .variable(getter);
        }
        if (userOptimisticLockPredicate != null) {
            builder.separator();
            ((Ast)userOptimisticLockPredicate).renderTo(builder);
        } else if (versionGetter != null) {
            builder.separator()
                    .sql(versionGetter.columnName())
                    .sql(" = ")
                    .variable(versionGetter);
        }
        builder.leave();

        MutationTrigger trigger = ctx.trigger;
        if (trigger != null) {
            if (batch.shape().getIdGetters().isEmpty()) {
                Set<ImmutableProp> keyProps = ctx.options.getKeyProps(ctx.path.getType());
                for (DraftSpi draft : batch.entities()) {
                    trigger.modifyEntityTable(
                            originalIdObjMap.get(Keys.keyOf(draft, keyProps)),
                            draft
                    );
                }
            } else {
                PropId idPropId = ctx.path.getType().getIdProp().getId();
                for (DraftSpi draft : batch.entities()) {
                    trigger.modifyEntityTable(
                            originalKeyObjMap.get(draft.__get(idPropId)),
                            draft
                    );
                }
            }
        }
        return execute(builder, batch, true);
    }

    public int upsert(Batch<DraftSpi> batch) {

        if (batch.entities().isEmpty()) {
            return 0;
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
        for (PropertyGetter getter : fullShape.getGetters()) {
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
                        "In order to insert object without id, the id generator must be identity or sequence"
                );
            }
        }

        List<PropertyGetter> insertedGetters = new ArrayList<>();
        if (sequenceIdGenerator != null) {
            insertedGetters.addAll(batch.shape().getIdGetters());
        }
        insertedGetters.addAll(batch.shape().getGetters());
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
        if (userOptimisticLockPredicate == null && versionGetter == null && ctx.path.getType().getVersionProp() != null) {
            ctx.throwNoVersionError();
        }

        BatchSqlBuilder builder = new BatchSqlBuilder(sqlClient);
        UpsertContextImpl updateContext = new UpsertContextImpl(
                builder,
                sequenceIdGenerator,
                insertedGetters,
                conflictGetters,
                updatedGetters,
                userOptimisticLockPredicate,
                versionGetter
        );
        sqlClient.getDialect().upsert(updateContext);
        return execute(builder, batch, true);
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

    private int execute(
            BatchSqlBuilder builder,
            Batch<DraftSpi> batch,
            boolean updatable
    ) {
        JSqlClientImplementor sqlClient = ctx.options.getSqlClient();
        PropertyGetter versionGetter = batch.shape().getVersionGetter();
        Tuple2<String, BatchSqlBuilder.VariableMapper> tuple = builder.build();
        try (Executor.BatchContext batchContext = sqlClient
                .getExecutor()
                .executeBatch(
                        sqlClient,
                        ctx.con,
                        tuple.get_1(),
                        batch.shape().getIdGetters().isEmpty() ? ctx.path.getType().getIdProp() : null
                )
        ) {
            BatchSqlBuilder.VariableMapper mapper = tuple.get_2();
            for (DraftSpi draft : batch.entities()) {
                batchContext.add(mapper.variables(draft));
            }
            int[] rowCounts = batchContext.execute();

            if (batch.shape().getIdGetters().isEmpty()) {
                Object[] generatedIds = batchContext.generatedIds();
                if (generatedIds.length != batch.entities().size()) {
                    throw new IllegalStateException(
                            "The inserted row count is " +
                                    batch.entities().size() +
                                    ", but the count of generated ids is " +
                                    generatedIds.length
                    );
                }
                PropId idPropId = ctx.path.getType().getIdProp().getId();
                int index = 0;
                for (DraftSpi draft : batch.entities()) {
                    draft.__set(idPropId, generatedIds[index++]);
                }
            }

            if (updatable && versionGetter != null) {
                PropId versionPropId = versionGetter.prop().getId();
                Iterator<DraftSpi> itr = batch.entities().iterator();
                for (int rowCount : rowCounts) {
                    DraftSpi draft = itr.next();
                    if (rowCount == 0) {
                        ctx.throwOptimisticLockError(draft);
                    }
                    Integer version = (Integer) draft.__get(versionPropId);
                    draft.__set(versionPropId, version + 1);
                }
            }

            int sumRowCount = 0;
            for (int rowCount : rowCounts) {
                if (rowCount != 0) {
                    sumRowCount++;
                }
            }
            return sumRowCount;
        }
    }

    private class UpsertContextImpl implements Dialect.UpsertContext {

        private final BatchSqlBuilder builder;

        private final SequenceIdGenerator sequenceIdGenerator;

        private final List<PropertyGetter> insertedGetters;

        private final List<PropertyGetter> conflictGetters;

        private final List<PropertyGetter> updatedGetters;

        private final Predicate userOptimisticLockPredicate;

        private final PropertyGetter versionGetter;

        private UpsertContextImpl(
                BatchSqlBuilder builder,
                SequenceIdGenerator sequenceIdGenerator,
                List<PropertyGetter> insertedGetters,
                List<PropertyGetter> conflictGetters,
                List<PropertyGetter> updatedGetters,
                Predicate userOptimisticLockPredicate,
                PropertyGetter versionGetter
        ) {
            this.builder = builder;
            this.sequenceIdGenerator = sequenceIdGenerator;
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
        public Dialect.UpsertContext appendTableName() {
            builder.sql(ctx.path.getType().getTableName(ctx.options.getSqlClient().getMetadataStrategy()));
            return this;
        }

        @Override
        public Dialect.UpsertContext appendInsertedColumns() {
            MetadataStrategy strategy = ctx.options.getSqlClient().getMetadataStrategy();
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
                builder.separator().sql(getter.columnName());
            }
            builder.leave();
            return this;
        }

        @Override
        public Dialect.UpsertContext appendConflictColumns() {
            MetadataStrategy strategy = ctx.options.getSqlClient().getMetadataStrategy();
            builder.enter(AbstractSqlBuilder.ScopeType.COMMA);
            for (PropertyGetter getter : conflictGetters) {
                builder.separator().sql(getter.columnName());
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
            MetadataStrategy strategy = ctx.options.getSqlClient().getMetadataStrategy();
            builder.enter(BatchSqlBuilder.ScopeType.COMMA);
            for (PropertyGetter getter : updatedGetters) {
                builder.separator()
                        .sql(getter.columnName())
                        .sql(" = ");
                if (getter.metadata().valueProp().isVersion() && ctx.options.getUserOptimisticLock(ctx.path.getType()) == null) {
                    builder.sql(prefix)
                            .sql(getter.columnName())
                            .sql(" + 1");
                } else {
                    builder.sql(prefix)
                            .sql(getter.columnName())
                            .sql(suffix);
                }
            }
            builder.leave();
            return this;
        }

        @Override
        public Dialect.UpsertContext appendOptimisticLockCondition() {
            MetadataStrategy strategy = ctx.options.getSqlClient().getMetadataStrategy();
            builder.withPropPrefix(ctx.path.getType().getTableName(strategy), () -> {
                if (userOptimisticLockPredicate != null) {
                    ((Ast)userOptimisticLockPredicate).renderTo(builder);
                } if (versionGetter != null) {
                    builder.prop(versionGetter.metadata().valueProp())
                            .sql(" = ")
                            .variable(versionGetter);
                }
            });
            return this;
        }
    }
}
