package org.babyfish.jimmer.sql.query;

import org.babyfish.jimmer.sql.ast.query.Order;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.embedded.*;
import org.babyfish.jimmer.sql.model.embedded.dto.TransformView;
import org.babyfish.jimmer.sql.model.embedded.dto.TransformView2;
import org.junit.jupiter.api.Test;

public class EmbeddedQueryTest extends AbstractQueryTest {

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
    public void testDtoWithFormula() {
        TransformTable table = TransformTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .select(table.fetch(TransformView2.class)),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, " +
                                    "tb_1_.`LEFT`, tb_1_.TOP, tb_1_.`RIGHT`, tb_1_.BOTTOM, " +
                                    "tb_1_.TARGET_RIGHT, tb_1_.TARGET_BOTTOM, tb_1_.TARGET_LEFT, tb_1_.TARGET_TOP " +
                                    "from TRANSFORM tb_1_"
                    );
                    ctx.rows(it -> {
                        assertContentEquals(
                                "[" +
                                        "--->TransformView2(" +
                                        "--->--->id=1, " +
                                        "--->--->source=TransformView2.TargetOf_source(" +
                                        "--->--->--->area=60000.0, " +
                                        "--->--->--->leftTop=TransformView2.TargetOf_source.TargetOf_leftTop(x=100)" +
                                        "--->--->), " +
                                        "--->--->target=TransformView2.TargetOf_target(" +
                                        "--->--->--->area=240000.0, " +
                                        "--->--->--->rightBottom=TransformView2.TargetOf_target.TargetOf_rightBottom(y=1000)" +
                                        "--->--->)" +
                                        "--->), " +
                                        "--->TransformView2(" +
                                        "--->--->id=2, " +
                                        "--->--->source=TransformView2.TargetOf_source(" +
                                        "--->--->--->area=60000.0, " +
                                        "--->--->--->leftTop=TransformView2.TargetOf_source.TargetOf_leftTop(x=150)" +
                                        "--->--->), " +
                                        "--->--->target=null" +
                                        "--->)" +
                                        "]",
                                it
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

    @Test
    public void testFormulaBaseOnEmbedded() {
        MachineTable table = MachineTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .select(
                                table.fetch(
                                        MachineFetcher.$
                                                .factoryCount()
                                )
                        ),
                ctx -> {
                    ctx.sql("select tb_1_.ID, tb_1_.factory_map from MACHINE tb_1_");
                    ctx.rows(
                            "[{\"id\":1,\"factoryCount\":2}]"
                    );
                }
        );
    }

    @Test
    public void testFormulaBaseOnEmbeddedAndFetchEmbedded() {
        MachineTable table = MachineTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .select(
                                table.fetch(
                                        MachineFetcher.$
                                                .factoryCount()
                                                .detail(
                                                        MachineDetailFetcher.$
                                                                .patents()
                                                )
                                )
                        ),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.patent_map, tb_1_.factory_map from MACHINE tb_1_"
                    );
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":1," +
                                    "--->--->\"detail\":{\"patents\":{\"p-1\":\"patent-1\",\"p-2\":\"patent-2\"}}," +
                                    "--->--->\"factoryCount\":2" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testFormulaBaseOnEmbeddedAndFetchDuplicatedEmbedded() {
        MachineTable table = MachineTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .select(
                                table.fetch(
                                        MachineFetcher.$
                                                .factoryCount()
                                                .detail(
                                                        MachineDetailFetcher.$
                                                                .patents()
                                                                .factories()
                                                )
                                )
                        ),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.patent_map, tb_1_.factory_map from MACHINE tb_1_"
                    );
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":1," +
                                    "--->--->\"detail\":{" +
                                    "--->--->--->\"factories\":{\"f-1\":\"factory-1\",\"f-2\":\"factory-2\"}," +
                                    "--->--->--->\"patents\":{\"p-1\":\"patent-1\",\"p-2\":\"patent-2\"}" +
                                    "--->--->}," +
                                    "--->--->\"factoryCount\":2" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testFormulaInEmbeddable() {
        TransformTable table = TransformTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .select(
                                table.fetch(
                                        TransformFetcher.$
                                                .source(
                                                        RectFetcher.$.area()
                                                )
                                                .target(
                                                        RectFetcher.$.area()
                                                )
                                )
                        ),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, " +
                                    "tb_1_.`LEFT`, tb_1_.TOP, tb_1_.`RIGHT`, tb_1_.BOTTOM, " +
                                    "tb_1_.TARGET_LEFT, tb_1_.TARGET_TOP, tb_1_.TARGET_RIGHT, tb_1_.TARGET_BOTTOM " +
                                    "from TRANSFORM tb_1_"
                    );
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":1," +
                                    "--->--->\"source\":{\"area\":60000.0}," +
                                    "--->--->\"target\":{\"area\":240000.0}" +
                                    "--->},{" +
                                    "--->--->\"id\":2," +
                                    "--->--->\"source\":{\"area\":60000.0}," +
                                    "--->--->\"target\":null" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testFormulaInEmbeddableAndDuplicatedFetch() {
        TransformTable table = TransformTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .select(
                                table.fetch(
                                        TransformFetcher.$
                                                .source(
                                                        RectFetcher.$
                                                                .area()
                                                                .leftTop(PointFetcher.$.x())
                                                )
                                                .target(
                                                        RectFetcher.$
                                                                .area()
                                                                .rightBottom(PointFetcher.$.y())
                                                )
                                )
                        ),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, " +
                                    "tb_1_.`LEFT`, tb_1_.TOP, tb_1_.`RIGHT`, tb_1_.BOTTOM, " +
                                    "tb_1_.TARGET_RIGHT, tb_1_.TARGET_BOTTOM, tb_1_.TARGET_LEFT, tb_1_.TARGET_TOP " +
                                    "from TRANSFORM tb_1_"
                    );
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":1," +
                                    "--->--->\"source\":{" +
                                    "--->--->--->\"leftTop\":{\"x\":100}," +
                                    "--->--->--->\"area\":60000.0" +
                                    "--->--->}," +
                                    "--->--->\"target\":{" +
                                    "--->--->--->\"rightBottom\":{\"y\":1000}," +
                                    "--->--->--->\"area\":240000.0" +
                                    "--->--->}" +
                                    "--->},{" +
                                    "--->--->\"id\":2," +
                                    "--->--->\"source\":{" +
                                    "--->--->--->\"leftTop\":{\"x\":150}," +
                                    "--->--->--->\"area\":60000.0" +
                                    "--->--->}," +
                                    "--->--->\"target\":null" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }
}
