package org.babyfish.jimmer.sql.query;

import org.babyfish.jimmer.sql.ast.query.Order;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.BookTable;
import org.babyfish.jimmer.sql.model.embedded.*;
import org.babyfish.jimmer.sql.model.embedded.dto.TransformView;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DynamicQueryTest extends AbstractQueryTest {

    @Test
    public void testReferenceProp() {
        BookTable table = BookTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .orderBy(
                                Order.makeOrders(
                                        table,
                                        "store.name asc nulls first;" +
                                                "name," +
                                                "edition desc"
                                )
                        )
                        .select(table),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                    "from BOOK tb_1_ " +
                                    "left join BOOK_STORE tb_2_ on tb_1_.STORE_ID = tb_2_.ID " +
                                    "order by tb_2_.NAME asc nulls first, " +
                                    "tb_1_.NAME asc, " +
                                    "tb_1_.EDITION desc"
                    );
                }
        );
    }

    @Test
    public void testEmbeddedProp() {
        TransformTable table = TransformTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .orderBy(
                                Order.makeOrders(
                                        table,
                                        "source.leftTop.x asc, target.rightBottom.y desc"
                                )
                        )
                        .select(table),
                ctx -> {
                    ctx.sql(
                            "select " +
                                    "tb_1_.ID, " +
                                    "tb_1_.`LEFT`, tb_1_.TOP, tb_1_.`RIGHT`, tb_1_.BOTTOM, " +
                                    "tb_1_.TARGET_LEFT, tb_1_.TARGET_TOP, tb_1_.TARGET_RIGHT, tb_1_.TARGET_BOTTOM " +
                                    "from TRANSFORM tb_1_ " +
                                    "order by tb_1_.`LEFT` asc, tb_1_.TARGET_BOTTOM desc"
                    );
                }
        );
    }

    @Test
    public void testObjectFetcher() {
        TransformTable table = TransformTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .select(
                                table.fetch(
                                        TransformFetcher.$
                                                .source(
                                                        RectFetcher.$
                                                                .leftTop(
                                                                        PointFetcher.$
                                                                                .x()
                                                                )
                                                )
                                                .target(
                                                        RectFetcher.$
                                                                .rightBottom(
                                                                        PointFetcher.$
                                                                                .y()
                                                                )
                                                )
                                )
                        ),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.`LEFT`, tb_1_.TARGET_BOTTOM " +
                                    "from TRANSFORM tb_1_"
                    );
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":1," +
                                    "--->--->\"source\":{" +
                                    "--->--->--->\"leftTop\":{\"x\":100}" +
                                    "--->--->}," +
                                    "--->--->\"target\":{" +
                                    "--->--->--->\"rightBottom\":{\"y\":1000}" +
                                    "--->--->}" +
                                    "--->},{" +
                                    "--->--->\"id\":2," +
                                    "--->--->\"source\":{" +
                                    "--->--->--->\"leftTop\":{\"x\":150}" +
                                    "--->--->}," +
                                    "--->--->\"target\":null" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testDto() {
        TransformTable table = TransformTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .select(table.fetch(TransformView.class)),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.`LEFT`, tb_1_.TARGET_BOTTOM from TRANSFORM tb_1_"
                    );
                    ctx.rows(rows -> {
                        assertContentEquals(
                                "[" +
                                        "--->TransformView(" +
                                        "--->--->id=1, " +
                                        "--->--->source=TransformView.TargetOf_source(" +
                                        "--->--->--->leftTop=TransformView.TargetOf_source.TargetOf_leftTop(" +
                                        "--->--->--->--->x=100" +
                                        "--->--->--->)" +
                                        "--->--->), " +
                                        "--->--->target=TransformView.TargetOf_target(" +
                                        "--->--->--->rightBottom=TransformView.TargetOf_target.TargetOf_rightBottom(" +
                                        "--->--->--->--->y=1000" +
                                        "--->--->--->)" +
                                        "--->--->)" +
                                        "--->), TransformView(" +
                                        "--->--->--->id=2, " +
                                        "--->--->--->source=TransformView.TargetOf_source(" +
                                        "--->--->--->--->leftTop=TransformView.TargetOf_source.TargetOf_leftTop(" +
                                        "--->--->--->--->x=150" +
                                        "--->--->--->)" +
                                        "--->--->), " +
                                        "--->--->target=null" +
                                        "--->)" +
                                        "]",
                                rows
                        );
                    });
                }
        );
    }

    @Test
    public void testEmbeddedJson() {
        MachineTable table = MachineTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .select(
                                table.fetch(
                                        MachineFetcher.$
                                                .detail(
                                                        MachineDetailFetcher.$
                                                                .factories()
                                                )
                                )
                        ),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.factory_map from MACHINE tb_1_"
                    );
                    ctx.rows(
                            "[{\"id\":1,\"detail\":{\"factories\":{\"f-1\":\"factory-1\",\"f-2\":\"factory-2\"}}}]"
                    );
                }
        );
    }
}
