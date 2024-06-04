package org.babyfish.jimmer.sql.ast.impl.mutation.save;

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
        if (batch.shape().idItems().isEmpty()) {
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

        Tuple2<String, TemplateBuilder.VariableMapper> tuple = builder.build();
        try (Executor.BatchContext batchContext = sqlClient
                .getExecutor()
                .executeBatch(
                        sqlClient,
                        ctx.con,
                        tuple.get_1(),
                        !batch.shape().idItems().isEmpty() ? null : ctx.path.getType().getIdProp()
                )
        ) {
            TemplateBuilder.VariableMapper mapper = tuple.get_2();
            for (DraftSpi draft : batch.entities()) {
                batchContext.add(mapper.variables(draft));
            }
            int[] rowCounts = batchContext.execute();
            if (batch.shape().idItems().isEmpty()) {
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
            return sum(rowCounts);
        }
    }

    public int update(Batch<DraftSpi> batch) {

        if (batch.shape().idItems().isEmpty()) {
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
        for (Shape.Item item : batch.shape().idItems()) {
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

        Tuple2<String, TemplateBuilder.VariableMapper> tuple = builder.build();
        try (Executor.BatchContext batchContext = sqlClient
                .getExecutor()
                .executeBatch(
                        sqlClient,
                        ctx.con,
                        tuple.get_1(),
                        null
                )
        ) {
            TemplateBuilder.VariableMapper mapper = tuple.get_2();
            for (DraftSpi draft : batch.entities()) {
                batchContext.add(mapper.variables(draft));
            }
            int[] rowCounts = batchContext.execute();
            int sumRowCount = 0;
            if (versionItem == null) {
                return sum(rowCounts);
            }
            PropId versionPropId = versionItem.prop().getId();
            Iterator<DraftSpi> itr = batch.entities().iterator();
            for (int rowCount : rowCounts) {
                DraftSpi draft = itr.next();
                if (rowCount == 0) {
                    ctx.throwOptimisticLockError(draft);
                }
                Integer version = (Integer) draft.__get(versionPropId);
                draft.__set(versionPropId, version + 1);
                sumRowCount += rowCount;
            }
            return sumRowCount;
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
            table = ((TableProxy<?>)table).__disableJoin(GENERAL_OPTIMISTIC_DISABLED_JOIN_REASON);
        }
        Predicate predicate = userOptimisticLock.predicate(
                (Table<Object>) table,
                OptimisticLockValueFactoryFactories.<Object>of()
        );
        return predicate;
    }

    private static int sum(int[] rowCounts) {
        int sumRowCount = 0;
        for (int rowCount : rowCounts) {
            sumRowCount += rowCount;
        }
        return sumRowCount;
    }
}
