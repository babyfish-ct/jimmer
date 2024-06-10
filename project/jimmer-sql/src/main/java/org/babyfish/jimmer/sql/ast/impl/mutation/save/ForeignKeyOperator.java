package org.babyfish.jimmer.sql.ast.impl.mutation.save;

import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.meta.MetadataStrategy;
import org.babyfish.jimmer.sql.runtime.Executor;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;

import java.util.List;

class ForeignKeyOperator {

    private final SaveContext ctx;

    private final String tableName;

    private final List<Shape.Item> foreignKeyItems;

    private final List<Shape.Item> idItems;

    ForeignKeyOperator(SaveContext ctx) {
        MetadataStrategy strategy = ctx.options.getSqlClient().getMetadataStrategy();
        Shape fullShape = Shape.fullOf(ctx.path.getType().getJavaClass());
        this.ctx = ctx;
        this.tableName = ctx.path.getType().getTableName(strategy);
        foreignKeyItems = fullShape.propItems(ctx.backReferenceProp);
        idItems = fullShape.getIdItems();
    }

    public int connect(Iterable<DraftSpi> drafts) {
        TemplateBuilder builder = new TemplateBuilder(ctx.options.getSqlClient());
        JSqlClientImplementor sqlClient = ctx.options.getSqlClient();
        MetadataStrategy strategy = sqlClient.getMetadataStrategy();
        builder.sql("update ")
                .sql(tableName)
                .enter(TemplateBuilder.ScopeType.SET);
        for (Shape.Item item : foreignKeyItems) {
            builder.separator().sql(item.columnName(strategy)).sql(" = ").variable(item);
        }
        builder.leave().enter(TemplateBuilder.ScopeType.WHERE);
        for (Shape.Item item : idItems) {
            builder.separator().sql(item.columnName(strategy)).sql(" = ").variable(item);
        }
        builder.leave();
        return execute(builder, drafts);
    }

    private int execute(TemplateBuilder builder, Iterable<DraftSpi> drafts) {
        Tuple2<String, TemplateBuilder.VariableMapper> tuple = builder.build();
        JSqlClientImplementor sqlClient = ctx.options.getSqlClient();
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
            for (DraftSpi draft : drafts) {
                batchContext.add(mapper.variables(draft));
            }
            int[] rowCounts = batchContext.execute();
            int sumRowCount = 0;
            for (int rowCount : rowCounts) {
                if (rowCount != 0) {
                    sumRowCount++;
                }
            }
            return sumRowCount;
        }
    }
}
