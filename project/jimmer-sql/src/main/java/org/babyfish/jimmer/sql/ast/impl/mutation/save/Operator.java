package org.babyfish.jimmer.sql.ast.impl.mutation.save;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.PropId;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.impl.Ast;
import org.babyfish.jimmer.sql.ast.impl.OptimisticLockValueFactoryFactories;
import org.babyfish.jimmer.sql.ast.impl.query.FilterLevel;
import org.babyfish.jimmer.sql.ast.impl.query.MutableRootQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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

        List<Shape.Item> defaultItems = new ArrayList<>();
        for (Shape.Item item : Shape.fullOf(batch.shape().getType().getJavaClass()).getItems()) {
            if (item.deepestProp().getDefaultValueRef() != null && !batch.shape().contains(item)) {
                defaultItems.add(item);
            }
        }
        SequenceIdGenerator sequenceIdGenerator = null;
        if (batch.shape().getIdItems().isEmpty()) {
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
        TemplateBuilder builder = new TemplateBuilder(sqlClient);
        builder.sql("insert into ")
                .sql(ctx.path.getType().getTableName(strategy))
                .enter(TemplateBuilder.ScopeType.TUPLE);
        if (sequenceIdGenerator != null) {
            builder.separator().sql(ctx.path.getType().getIdProp().<SingleColumn>getStorage(strategy).getName());
        }
        for (Shape.Item item : batch.shape().getItems()) {
            builder.separator().sql(item.columnName(strategy));
        }
        for (Shape.Item defaultItem : defaultItems) {
            builder.separator().sql(defaultItem.columnName(strategy));
        }
        builder.leave().sql(" values").enter(TemplateBuilder.ScopeType.TUPLE);
        if (sequenceIdGenerator != null) {
            builder.separator()
                    .sql("(")
                    .sql(
                            sqlClient.getDialect().getSelectIdFromSequenceSql(sequenceIdGenerator.getSequenceName())
                    )
                    .sql(")");
        }
        for (Shape.Item item : batch.shape().getItems()) {
            builder.separator().variable(item);
        }
        for (Shape.Item defaultItem : defaultItems) {
            builder.separator().defaultVariable(defaultItem);
        }
        builder.leave();

        return execute(builder, batch, false);
    }

    public int update(Batch<DraftSpi> batch) {

        if (batch.shape().getIdItems().isEmpty()) {
            throw new IllegalArgumentException("Cannot update batch whose shape does not have id");
        }
        if (batch.entities().isEmpty()) {
            return 0;
        }

        JSqlClientImplementor sqlClient = ctx.options.getSqlClient();
        MetadataStrategy strategy = sqlClient.getMetadataStrategy();
        Predicate userOptimisticLockPredicate = userLockOptimisticPredicate();
        Shape.Item versionItem = batch.shape().getVersionItem();
        if (userOptimisticLockPredicate == null && versionItem == null && ctx.path.getType().getVersionProp() != null) {
            ctx.throwNoVersionError();
        }

        TemplateBuilder builder = new TemplateBuilder(sqlClient);
        builder.sql("update ")
                .sql(ctx.path.getType().getTableName(strategy))
                .enter(TemplateBuilder.ScopeType.SET);
        for (Shape.Item item : batch.shape().getItems()) {
            if (item.prop().isId()) {
                continue;
            }
            if (item.prop().isVersion() && userOptimisticLockPredicate == null) {
                continue;
            }
            builder.separator()
                    .sql(item.columnName(strategy))
                    .sql(" = ")
                    .variable(item);
        }
        if (userOptimisticLockPredicate == null && versionItem != null) {
            builder.separator()
                    .sql(versionItem.columnName(strategy))
                    .sql(" = ")
                    .sql(versionItem.columnName(strategy))
                    .sql(" + 1");
        }
        builder.leave().enter(TemplateBuilder.ScopeType.WHERE);
        for (Shape.Item item : batch.shape().getIdItems()) {
            builder.separator()
                    .sql(item.columnName(strategy))
                    .sql(" = ")
                    .variable(item);
        }
        if (userOptimisticLockPredicate != null) {
            builder.separator();
            ((Ast)userOptimisticLockPredicate).renderTo(builder);
        } else if (versionItem != null) {
            builder.separator()
                    .sql(versionItem.columnName(strategy))
                    .sql(" = ")
                    .variable(versionItem);
        }
        builder.leave();

        return execute(builder, batch, true);
    }

    public int upsert(Batch<DraftSpi> batch) {

        if (batch.entities().isEmpty()) {
            return 0;
        }

        JSqlClientImplementor sqlClient = ctx.options.getSqlClient();

        List<Shape.Item> defaultItems = new ArrayList<>();
        for (Shape.Item item : Shape.fullOf(batch.shape().getType().getJavaClass()).getItems()) {
            if (item.deepestProp().getDefaultValueRef() != null && !batch.shape().contains(item)) {
                defaultItems.add(item);
            }
        }
        SequenceIdGenerator sequenceIdGenerator = null;
        if (batch.shape().getIdItems().isEmpty()) {
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

        List<Shape.Item> insertedItems = new ArrayList<>();
        if (sequenceIdGenerator != null) {
            insertedItems.addAll(batch.shape().getIdItems());
        }
        insertedItems.addAll(batch.shape().getItems());
        insertedItems.addAll(defaultItems);

        List<Shape.Item> conflictItems = new ArrayList<>();
        if (!batch.shape().getIdItems().isEmpty()) {
            conflictItems.addAll(batch.shape().getIdItems());
        } else {
            Set<ImmutableProp> keyProps = ctx.options.getKeyProps(ctx.path.getType());
            for (Shape.Item item : Shape.fullOf(ctx.path.getType().getJavaClass()).getItems()) {
                if (keyProps.contains(item.prop())) {
                    conflictItems.add(item);
                }
            }
        }

        List<Shape.Item> updatedItems = new ArrayList<>();
        for (Shape.Item item : batch.shape().getItems()) {
            if (!conflictItems.contains(item)) {
                updatedItems.add(item);
            }
        }
        for (Shape.Item defaultItem : defaultItems) {
            if (!conflictItems.contains(defaultItem)) {
                updatedItems.add(defaultItem);
            }
        }

        Predicate userOptimisticLockPredicate = userLockOptimisticPredicate();
        Shape.Item versionItem = batch.shape().getVersionItem();
        if (userOptimisticLockPredicate == null && versionItem == null && ctx.path.getType().getVersionProp() != null) {
            ctx.throwNoVersionError();
        }

        TemplateBuilder builder = new TemplateBuilder(sqlClient);
        UpsertContextImpl updateContext = new UpsertContextImpl(
                builder,
                sequenceIdGenerator,
                insertedItems,
                conflictItems,
                updatedItems,
                userOptimisticLockPredicate,
                versionItem
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
            TemplateBuilder builder,
            Batch<DraftSpi> batch,
            boolean updatable
    ) {
        JSqlClientImplementor sqlClient = ctx.options.getSqlClient();
        Shape.Item versionItem = batch.shape().getVersionItem();
        Tuple2<String, TemplateBuilder.VariableMapper> tuple = builder.build();
        try (Executor.BatchContext batchContext = sqlClient
                .getExecutor()
                .executeBatch(
                        sqlClient,
                        ctx.con,
                        tuple.get_1(),
                        batch.shape().getIdItems().isEmpty() ? ctx.path.getType().getIdProp() : null
                )
        ) {
            TemplateBuilder.VariableMapper mapper = tuple.get_2();
            for (DraftSpi draft : batch.entities()) {
                batchContext.add(mapper.variables(draft));
            }
            int[] rowCounts = batchContext.execute();

            if (batch.shape().getIdItems().isEmpty()) {
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

            if (updatable && versionItem != null) {
                PropId versionPropId = versionItem.prop().getId();
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
                sumRowCount += rowCount;
            }
            return sumRowCount;
        }
    }

    private class UpsertContextImpl implements Dialect.UpsertContext {

        private final TemplateBuilder builder;

        private final SequenceIdGenerator sequenceIdGenerator;

        private final List<Shape.Item> insertedItems;

        private final List<Shape.Item> conflictItems;

        private final List<Shape.Item> updatedItems;

        private final Predicate userOptimisticLockPredicate;

        private final Shape.Item versionItem;

        private UpsertContextImpl(
                TemplateBuilder builder,
                SequenceIdGenerator sequenceIdGenerator,
                List<Shape.Item> insertedItems,
                List<Shape.Item> conflictItems,
                List<Shape.Item> updatedItems,
                Predicate userOptimisticLockPredicate,
                Shape.Item versionItem
        ) {
            this.builder = builder;
            this.sequenceIdGenerator = sequenceIdGenerator;
            this.insertedItems = insertedItems;
            this.conflictItems = conflictItems;
            this.updatedItems = updatedItems;
            this.userOptimisticLockPredicate = userOptimisticLockPredicate;
            this.versionItem = versionItem;
        }

        @Override
        public boolean hasUpdatedColumns() {
            return !updatedItems.isEmpty();
        }

        @Override
        public boolean hasOptimisticLock() {
            return userOptimisticLockPredicate != null || versionItem != null;
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
            builder.enter(TemplateBuilder.ScopeType.COMMA);
            if (sequenceIdGenerator != null) {
                builder.separator()
                        .sql("(")
                        .sql(
                                builder.sqlClient
                                        .getDialect()
                                        .getSelectIdFromSequenceSql(sequenceIdGenerator.getSequenceName())
                        )
                        .sql(")");
            }
            for (Shape.Item item : insertedItems) {
                builder.separator().sql(item.columnName(strategy));
            }
            builder.leave();
            return this;
        }

        @Override
        public Dialect.UpsertContext appendConflictColumns() {
            MetadataStrategy strategy = ctx.options.getSqlClient().getMetadataStrategy();
            builder.enter(TemplateBuilder.ScopeType.COMMA);
            for (Shape.Item item : conflictItems) {
                builder.separator().sql(item.columnName(strategy));
            }
            builder.leave();
            return this;
        }

        @Override
        public Dialect.UpsertContext appendInsertingValues() {
            builder.enter(TemplateBuilder.ScopeType.COMMA);
            for (Shape.Item item : insertedItems) {
                builder.separator().variable(item);
            }
            builder.leave();
            return this;
        }

        @Override
        public Dialect.UpsertContext appendUpdatingAssignments(String prefix, String suffix) {
            MetadataStrategy strategy = ctx.options.getSqlClient().getMetadataStrategy();
            builder.enter(TemplateBuilder.ScopeType.COMMA);
            for (Shape.Item item : updatedItems) {
                builder.separator()
                        .sql(item.columnName(strategy))
                        .sql(" = ");
                if (item.deepestProp().isVersion() && ctx.options.getUserOptimisticLock(ctx.path.getType()) == null) {
                    builder.sql(prefix)
                            .sql(item.columnName(strategy))
                            .sql(" + 1");
                } else {
                    builder.sql(prefix)
                            .sql(item.columnName(strategy))
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
                } if (versionItem != null) {
                    builder.prop(versionItem.deepestProp())
                            .sql(" = ")
                            .variable(versionItem);
                }
            });
            return this;
        }
    }
}
