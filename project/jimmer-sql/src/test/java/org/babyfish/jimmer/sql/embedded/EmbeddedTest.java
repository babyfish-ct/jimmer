package org.babyfish.jimmer.sql.embedded;

import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.embedded.*;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

public class EmbeddedTest extends AbstractQueryTest {

    private static final String ROWS = "[" +
            "--->{" +
            "--->--->\"id\":1," +
            "--->--->\"source\":{" +
            "--->--->--->\"leftTop\":{\"x\":100,\"y\":120}," +
            "--->--->--->\"rightBottom\":{\"x\":400,\"y\":320}" +
            "--->--->}," +
            "--->--->\"target\":{" +
            "--->--->--->\"leftTop\":{\"x\":800,\"y\":600}," +
            "--->--->--->\"rightBottom\":{\"x\":1400,\"y\":1000}" +
            "--->--->}" +
            "--->}" +
            "]";

    @Test
    public void testFindById() {
        TransformTable transform = TransformTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(transform)
                        .where(transform.id().eq(1L))
                        .select(transform),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, " +
                                    "tb_1_.`LEFT`, tb_1_.TOP, tb_1_.`RIGHT`, tb_1_.BOTTOM, " +
                                    "tb_1_.TARGET_LEFT, tb_1_.TARGET_TOP, tb_1_.TARGET_RIGHT, tb_1_.TARGET_BOTTOM " +
                                    "from TRANSFORM as tb_1_ " +
                                    "where tb_1_.ID = ?"
                    ).variables(1L);
                    ctx.rows(ROWS);
                }
        );
    }

    @Test
    public void testFindNullById() {
        TransformTable transform = TransformTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(transform)
                        .where(transform.id().eq(2L))
                        .select(transform),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, " +
                                    "tb_1_.`LEFT`, tb_1_.TOP, tb_1_.`RIGHT`, tb_1_.BOTTOM, " +
                                    "tb_1_.TARGET_LEFT, tb_1_.TARGET_TOP, tb_1_.TARGET_RIGHT, tb_1_.TARGET_BOTTOM " +
                                    "from TRANSFORM as tb_1_ " +
                                    "where tb_1_.ID = ?"
                    ).variables(2L);
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":2," +
                                    "--->--->\"source\":{" +
                                    "--->--->--->\"leftTop\":{\"x\":150,\"y\":170}," +
                                    "--->--->--->\"rightBottom\":{\"x\":450,\"y\":370}" +
                                    "--->--->}," +
                                    "--->--->\"target\":null" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testFindBySourceLeftTopX() {
        TransformTable transform = TransformTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(transform)
                        .where(transform.source().leftTop().x().eq(100L))
                        .select(transform),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, " +
                                    "tb_1_.`LEFT`, tb_1_.TOP, tb_1_.`RIGHT`, tb_1_.BOTTOM, " +
                                    "tb_1_.TARGET_LEFT, tb_1_.TARGET_TOP, tb_1_.TARGET_RIGHT, tb_1_.TARGET_BOTTOM " +
                                    "from TRANSFORM as tb_1_ " +
                                    "where tb_1_.`LEFT` = ?"
                    ).variables(100L);
                    ctx.rows(ROWS);
                }
        );
    }

    @Test
    public void testFindBySourceLeftTop() {
        TransformTable transform = TransformTable.$;
        Point point = PointDraft.$.produce(draft -> draft.setX(100).setY(120));
        executeAndExpect(
                getSqlClient()
                        .createQuery(transform)
                        .where(transform.source().leftTop().eq(point))
                        .select(transform),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, " +
                                    "tb_1_.`LEFT`, tb_1_.TOP, tb_1_.`RIGHT`, tb_1_.BOTTOM, " +
                                    "tb_1_.TARGET_LEFT, tb_1_.TARGET_TOP, tb_1_.TARGET_RIGHT, tb_1_.TARGET_BOTTOM " +
                                    "from TRANSFORM as tb_1_ " +
                                    "where (tb_1_.`LEFT`, tb_1_.TOP) = (?, ?)"
                    ).variables(100L, 120L);
                    ctx.rows(ROWS);
                }
        );
    }

    @Test
    public void testFindBySource() {
        TransformTable transform = TransformTable.$;
        Rect rect = RectDraft.$.produce(draft ->
                draft
                        .setLeftTop(leftTop -> leftTop.setX(100).setY(120))
                        .setRightBottom(leftTop -> leftTop.setX(400).setY(320))
        );
        executeAndExpect(
                getSqlClient()
                        .createQuery(transform)
                        .where(transform.source().eq(rect))
                        .select(transform),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, " +
                                    "tb_1_.`LEFT`, tb_1_.TOP, tb_1_.`RIGHT`, tb_1_.BOTTOM, " +
                                    "tb_1_.TARGET_LEFT, tb_1_.TARGET_TOP, tb_1_.TARGET_RIGHT, tb_1_.TARGET_BOTTOM " +
                                    "from TRANSFORM as tb_1_ " +
                                    "where (tb_1_.`LEFT`, tb_1_.TOP, tb_1_.`RIGHT`, tb_1_.BOTTOM) = (?, ?, ?, ?)"
                    ).variables(100L, 120L, 400L, 320L);
                    ctx.rows(ROWS);
                }
        );
    }

    @Test
    public void testSelectLevel1() {
        TransformTable transform = TransformTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(transform)
                        .where(transform.id().eq(1L))
                        .select(
                                transform.id(),
                                transform.source(),
                                transform.target()
                        ),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, " +
                                    "tb_1_.`LEFT`, tb_1_.TOP, tb_1_.`RIGHT`, tb_1_.BOTTOM, " +
                                    "tb_1_.TARGET_LEFT, tb_1_.TARGET_TOP, tb_1_.TARGET_RIGHT, tb_1_.TARGET_BOTTOM " +
                                    "from TRANSFORM as tb_1_ " +
                                    "where tb_1_.ID = ?"
                    );
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"_1\":1," +
                                    "--->--->\"_2\":{" +
                                    "--->--->--->\"leftTop\":{\"x\":100,\"y\":120}," +
                                    "--->--->--->\"rightBottom\":{\"x\":400,\"y\":320}" +
                                    "--->--->}," +
                                    "--->--->\"_3\":{" +
                                    "--->--->--->\"leftTop\":{\"x\":800,\"y\":600}," +
                                    "--->--->--->\"rightBottom\":{\"x\":1400,\"y\":1000}" +
                                    "--->--->}" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testSelectLevel2() {
        TransformTable transform = TransformTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(transform)
                        .where(transform.id().eq(1L))
                        .select(
                                transform.id(),
                                transform.source().leftTop(),
                                transform.source().rightBottom(),
                                transform.target().leftTop(),
                                transform.target().rightBottom()
                        ),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, " +
                                    "tb_1_.`LEFT`, tb_1_.TOP, tb_1_.`RIGHT`, tb_1_.BOTTOM, " +
                                    "tb_1_.TARGET_LEFT, tb_1_.TARGET_TOP, tb_1_.TARGET_RIGHT, tb_1_.TARGET_BOTTOM " +
                                    "from TRANSFORM as tb_1_ " +
                                    "where tb_1_.ID = ?"
                    );
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"_1\":1," +
                                    "--->--->\"_2\":{\"x\":100,\"y\":120}," +
                                    "--->--->\"_3\":{\"x\":400,\"y\":320}," +
                                    "--->--->\"_4\":{\"x\":800,\"y\":600}," +
                                    "--->--->\"_5\":{\"x\":1400,\"y\":1000}" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testSelectLevel3() {
        TransformTable transform = TransformTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(transform)
                        .where(transform.id().eq(1L))
                        .select(
                                transform.id(),
                                transform.source().leftTop().x(),
                                transform.source().leftTop().y(),
                                transform.source().rightBottom().x(),
                                transform.source().rightBottom().y(),
                                transform.target().leftTop().x(),
                                transform.target().leftTop().y(),
                                transform.target().rightBottom().x(),
                                transform.target().rightBottom().y()
                        ),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, " +
                                    "tb_1_.`LEFT`, tb_1_.TOP, tb_1_.`RIGHT`, tb_1_.BOTTOM, " +
                                    "tb_1_.TARGET_LEFT, tb_1_.TARGET_TOP, tb_1_.TARGET_RIGHT, tb_1_.TARGET_BOTTOM " +
                                    "from TRANSFORM as tb_1_ " +
                                    "where tb_1_.ID = ?"
                    );
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"_1\":1," +
                                    "--->--->\"_2\":100," +
                                    "--->--->\"_3\":120," +
                                    "--->--->\"_4\":400," +
                                    "--->--->\"_5\":320," +
                                    "--->--->\"_6\":800," +
                                    "--->--->\"_7\":600," +
                                    "--->--->\"_8\":1400," +
                                    "--->--->\"_9\":1000" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void findByCompositeIds() {
        List<OrderItemId> orderItemIds = Arrays.asList(
                OrderItemIdDraft.$.produce(draft -> draft.setA(1).setB(1).setC(1)),
                OrderItemIdDraft.$.produce(draft -> draft.setA(1).setB(1).setC(2))
        );
        anyAndExpect(
                con -> getSqlClient()
                        .getEntities()
                        .forConnection(con)
                        .findByIds(OrderItem.class, orderItemIds),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ORDER_ITEM_A, tb_1_.ORDER_ITEM_B, tb_1_.ORDER_ITEM_C, " +
                                    "--->tb_1_.NAME, tb_1_.FK_ORDER_X, tb_1_.FK_ORDER_Y " +
                                    "from ORDER_ITEM as tb_1_ " +
                                    "where (" +
                                    "--->tb_1_.ORDER_ITEM_A, tb_1_.ORDER_ITEM_B, tb_1_.ORDER_ITEM_C" +
                                    ") in (" +
                                    "--->(?, ?, ?), (?, ?, ?)" +
                                    ")"
                    );
                }
        );
    }

    @Test
    public void testNullCompositeId() {
        OrderItemTable orderItem = OrderItemTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(orderItem)
                        .where(orderItem.order().isNull())
                        .select(orderItem),
                ctx -> {
                    ctx.sql(
                            "select " +
                                    "--->tb_1_.ORDER_ITEM_A, tb_1_.ORDER_ITEM_B, tb_1_.ORDER_ITEM_C, " +
                                    "--->tb_1_.NAME, tb_1_.FK_ORDER_X, tb_1_.FK_ORDER_Y " +
                                    "from ORDER_ITEM as tb_1_ " +
                                    "where tb_1_.FK_ORDER_X is null and tb_1_.FK_ORDER_Y is null"
                    );
                }
        );
    }

    @Test
    public void testNullSubId() {
        OrderItemTable orderItem = OrderItemTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(orderItem)
                        .where(orderItem.order().id().x().isNull())
                        .select(orderItem),
                ctx -> {
                    ctx.sql(
                            "select " +
                                    "--->tb_1_.ORDER_ITEM_A, tb_1_.ORDER_ITEM_B, tb_1_.ORDER_ITEM_C, " +
                                    "--->tb_1_.NAME, tb_1_.FK_ORDER_X, tb_1_.FK_ORDER_Y " +
                                    "from ORDER_ITEM as tb_1_ " +
                                    "where tb_1_.FK_ORDER_X is null"
                    );
                }
        );
    }

    @Test
    public void testNonNullCompositeId() {
        OrderItemTable orderItem = OrderItemTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(orderItem)
                        .where(orderItem.order().isNotNull())
                        .select(orderItem),
                ctx -> {
                    ctx.sql(
                            "select " +
                                    "--->tb_1_.ORDER_ITEM_A, tb_1_.ORDER_ITEM_B, tb_1_.ORDER_ITEM_C, " +
                                    "--->tb_1_.NAME, tb_1_.FK_ORDER_X, tb_1_.FK_ORDER_Y " +
                                    "from ORDER_ITEM as tb_1_ " +
                                    "where tb_1_.FK_ORDER_X is not null and tb_1_.FK_ORDER_Y is not null"
                    );
                }
        );
    }

    @Test
    public void testNonNullSubId() {
        OrderItemTable orderItem = OrderItemTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(orderItem)
                        .where(orderItem.order().id().x().isNotNull())
                        .select(orderItem),
                ctx -> {
                    ctx.sql(
                            "select " +
                                    "--->tb_1_.ORDER_ITEM_A, tb_1_.ORDER_ITEM_B, tb_1_.ORDER_ITEM_C, " +
                                    "--->tb_1_.NAME, tb_1_.FK_ORDER_X, tb_1_.FK_ORDER_Y " +
                                    "from ORDER_ITEM as tb_1_ " +
                                    "where tb_1_.FK_ORDER_X is not null"
                    );
                }
        );
    }
}
