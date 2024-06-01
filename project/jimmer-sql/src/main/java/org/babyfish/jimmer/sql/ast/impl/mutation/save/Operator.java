package org.babyfish.jimmer.sql.ast.impl.mutation.save;

import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.meta.MetadataStrategy;
import org.babyfish.jimmer.sql.runtime.Executor;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class Operator {

    final SaveContext ctx;

    Operator(SaveContext ctx) {
        this.ctx = ctx;
    }

    public int insert(Batch<DraftSpi> batch) {

        JSqlClientImplementor sqlClient = ctx.options.getSqlClient();

        List<SaveShape.Item> defaultItems = new ArrayList<>();
        for (SaveShape.Item item : SaveShape.fullOf(batch.shape().getType().getJavaClass()).getItems()) {
            if (item.deepestProp().getDefaultValueRef() != null && !batch.shape().contains(item)) {
                defaultItems.add(item);
            }
        }

        MetadataStrategy strategy = sqlClient.getMetadataStrategy();
        TemplateBuilder builder = new TemplateBuilder(sqlClient);
        builder.sql("insert into ")
                .sql(ctx.path.getType().getTableName(strategy))
                .enter();
        for (SaveShape.Item item : batch.shape().getItems()) {
            builder.separator().sql(item.columnName(strategy));
        }
        for (SaveShape.Item defaultItem : defaultItems) {
            builder.separator().sql(defaultItem.columnName(strategy));
        }
        builder.leave().sql("values").enter();
        for (SaveShape.Item item : batch.shape().getItems()) {
            builder.separator().variable(item);
        }
        for (SaveShape.Item defaultItem : defaultItems) {
            builder.separator().defaultVariable(defaultItem);
        }
        builder.leave();

        Tuple2<String, TemplateBuilder.VariableMapper> tuple = builder.build();
        try (Executor.BatchContext batchContext = sqlClient
                .getExecutor()
                .executeBatch(sqlClient, ctx.con, tuple.get_1(), null)
        ) {
            TemplateBuilder.VariableMapper mapper = tuple.get_2();
            for (DraftSpi draft : batch.entities()) {
                batchContext.add(mapper.variables(draft));
            }
            int[] rows = batchContext.execute();
            int rowCount = 0;
            for (int row : rows) {
                rowCount += row;
            }
            return rowCount;
        }
    }
}
