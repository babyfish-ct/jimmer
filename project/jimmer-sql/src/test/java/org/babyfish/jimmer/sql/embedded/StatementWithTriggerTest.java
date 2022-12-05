package org.babyfish.jimmer.sql.embedded;

import org.babyfish.jimmer.sql.model.embedded.*;
import org.babyfish.jimmer.sql.trigger.AbstractTriggerTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class StatementWithTriggerTest extends AbstractTriggerTest {

    @Test
    public void testUpdateFailed() {
        TransformTable transform = TransformTable.$;
        IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            getSqlClient()
                    .createUpdate(transform)
                    .set(transform.source().leftTop(), PointDraft.$.produce(pt -> pt.setX(1).setY(2)))
                    .where(transform.id().eq(1L));
        });
        Assertions.assertEquals(
                "The property \"org.babyfish.jimmer.sql.model.embedded.Transform.source.leftTop\" is embedded, " +
                        "it cannot be used as the assignment target of update statement",
                ex.getMessage()
        );
        assertEvents();
    }

    @Test
    public void testUpdate() {
        TransformTable transform = TransformTable.$;
        executeAndExpectRowCount(
                getSqlClient()
                        .createUpdate(transform)
                        .set(transform.target().rightBottom().y(), transform.target().rightBottom().y().plus(100L))
                        .where(transform.id().eq(1L)),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select " +
                                        "--->tb_1_.ID, tb_1_.`LEFT`, tb_1_.TOP, tb_1_.`RIGHT`, tb_1_.BOTTOM, " +
                                        "--->tb_1_.TARGET_LEFT, tb_1_.TARGET_TOP, tb_1_.TARGET_RIGHT, tb_1_.TARGET_BOTTOM " +
                                        "from TRANSFORM as tb_1_ " +
                                        "where tb_1_.ID = ?"
                        );
                        it.variables(1L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update TRANSFORM tb_1_ " +
                                        "set TARGET_BOTTOM = tb_1_.TARGET_BOTTOM + ? " +
                                        "where tb_1_.ID in (?)"
                        );
                        it.variables(100L, 1L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select " +
                                        "--->tb_1_.ID, tb_1_.`LEFT`, tb_1_.TOP, tb_1_.`RIGHT`, tb_1_.BOTTOM, " +
                                        "--->tb_1_.TARGET_LEFT, tb_1_.TARGET_TOP, tb_1_.TARGET_RIGHT, tb_1_.TARGET_BOTTOM " +
                                        "from TRANSFORM as tb_1_ " +
                                        "where tb_1_.ID in (?)"
                        );
                        it.variables(1L);
                    });
                    ctx.rowCount(1);
                }
        );
        assertEvents(
                "Event{" +
                        "--->oldEntity={" +
                        "--->--->\"id\":1," +
                        "--->--->\"source\":{" +
                        "--->--->--->\"leftTop\":{\"x\":100,\"y\":120}," +
                        "--->--->--->\"rightBottom\":{\"x\":400,\"y\":320}" +
                        "--->--->}," +
                        "--->--->\"target\":{" +
                        "--->--->--->\"leftTop\":{\"x\":800,\"y\":600}," +
                        "--->--->--->\"rightBottom\":{\"x\":1400,\"y\":1000}" +
                        "--->--->}" +
                        "--->}, " +
                        "--->newEntity={" +
                        "--->--->\"id\":1," +
                        "--->--->\"source\":{" +
                        "--->--->--->\"leftTop\":{\"x\":100,\"y\":120}," +
                        "--->--->--->\"rightBottom\":{\"x\":400,\"y\":320}" +
                        "--->--->}," +
                        "--->--->\"target\":{" +
                        "--->--->--->\"leftTop\":{\"x\":800,\"y\":600}," +
                        "--->--->--->\"rightBottom\":{\"x\":1400,\"y\":1100}" +
                        "--->--->}" +
                        "--->}, " +
                        "--->reason=null" +
                        "}"
        );
    }

    @Test
    public void testUpdateByCompositeId() {
        OrderItemTable orderItem = OrderItemTable.$;
        executeAndExpectRowCount(
                getSqlClient()
                        .createUpdate(orderItem)
                        .set(orderItem.name(), orderItem.name().upper())
                        .where(orderItem.id().eq(OrderItemIdDraft.$.produce(id -> id.setA(1).setB(1).setC(1)))),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select " +
                                        "--->tb_1_.ORDER_ITEM_A, tb_1_.ORDER_ITEM_B, tb_1_.ORDER_ITEM_C, " +
                                        "--->tb_1_.NAME, tb_1_.FK_ORDER_X, tb_1_.FK_ORDER_Y " +
                                        "from ORDER_ITEM as tb_1_ " +
                                        "where (tb_1_.ORDER_ITEM_A, tb_1_.ORDER_ITEM_B, tb_1_.ORDER_ITEM_C) = (?, ?, ?)"
                        );
                        it.variables(1, 1, 1);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update ORDER_ITEM tb_1_ " +
                                        "set NAME = upper(tb_1_.NAME) " +
                                        "where (" +
                                        "--->tb_1_.ORDER_ITEM_A, tb_1_.ORDER_ITEM_B, tb_1_.ORDER_ITEM_C" +
                                        ") in (" +
                                        "--->(?, ?, ?)" +
                                        ")");
                        it.variables(1, 1, 1);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select " +
                                        "--->tb_1_.ORDER_ITEM_A, tb_1_.ORDER_ITEM_B, tb_1_.ORDER_ITEM_C, " +
                                        "--->tb_1_.NAME, tb_1_.FK_ORDER_X, tb_1_.FK_ORDER_Y " +
                                        "from ORDER_ITEM as tb_1_ " +
                                        "where (" +
                                        "--->tb_1_.ORDER_ITEM_A, tb_1_.ORDER_ITEM_B, tb_1_.ORDER_ITEM_C" +
                                        ") in (" +
                                        "--->(?, ?, ?)" +
                                        ")"
                        );
                        it.variables(1, 1, 1);
                    });
                    ctx.rowCount(1);
                }
        );
        assertEvents(
                "Event{" +
                        "--->oldEntity={" +
                        "--->--->\"id\":{\"a\":1,\"b\":1,\"c\":1}," +
                        "--->--->\"name\":\"order-item-1-1\"," +
                        "--->--->\"order\":{" +
                        "--->--->--->\"id\":{\"x\":\"001\",\"y\":\"001\"}" +
                        "--->--->}" +
                        "--->}, " +
                        "--->newEntity={" +
                        "--->--->\"id\":{\"a\":1,\"b\":1,\"c\":1}," +
                        "--->--->\"name\":\"ORDER-ITEM-1-1\"," +
                        "--->--->\"order\":{" +
                        "--->--->--->\"id\":{\"x\":\"001\",\"y\":\"001\"}" +
                        "--->--->}" +
                        "--->}, " +
                        "--->reason=null" +
                        "}"
        );
    }

    @Test
    public void testDelete() {
        OrderTable order = OrderTable.$;
        executeAndExpectRowCount(
                getSqlClient()
                        .createDelete(order)
                        .where(order.id().eq(OrderIdDraft.$.produce(id -> id.setX("001").setY("001")))),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ORDER_X, tb_1_.ORDER_Y, tb_1_.NAME " +
                                        "from ORDER_ as tb_1_ " +
                                        "where (tb_1_.ORDER_X, tb_1_.ORDER_Y) = (?, ?)"
                        );
                        it.variables("001", "001");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select ORDER_ITEM_A, ORDER_ITEM_B, ORDER_ITEM_C " +
                                        "from ORDER_ITEM " +
                                        "where (FK_ORDER_X, FK_ORDER_Y) in ((?, ?))"
                        );
                        it.variables("001", "001");
                    });
                    ctx.statement(it ->{
                        it.sql(
                                "select " +
                                        "--->FK_ORDER_ITEM_A, FK_ORDER_ITEM_B, FK_ORDER_ITEM_C, " +
                                        "--->FK_PRODUCT_ALPHA, FK_PRODUCT_BETA " +
                                        "from ORDER_ITEM_PRODUCT_MAPPING where (" +
                                        "--->FK_ORDER_ITEM_A, FK_ORDER_ITEM_B, FK_ORDER_ITEM_C" +
                                        ") in (" +
                                        "--->(?, ?, ?), (?, ?, ?)" +
                                        ")"
                        );
                        it.variables(1, 1, 1, 1, 1, 2);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from ORDER_ITEM_PRODUCT_MAPPING " +
                                        "where (" +
                                        "--->FK_ORDER_ITEM_A, FK_ORDER_ITEM_B, FK_ORDER_ITEM_C, " +
                                        "--->FK_PRODUCT_ALPHA, FK_PRODUCT_BETA" +
                                        ") in (" +
                                        "--->(?, ?, ?, ?, ?), (?, ?, ?, ?, ?), (?, ?, ?, ?, ?), (?, ?, ?, ?, ?)" +
                                        ")"
                        );
                        it.variables(
                                1, 1, 1, "00A", "00A",
                                1, 1, 1, "00B", "00A",
                                1, 1, 2, "00A", "00A",
                                1, 1, 2, "00A", "00B"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select " +
                                        "--->tb_1_.ORDER_ITEM_A, tb_1_.ORDER_ITEM_B, tb_1_.ORDER_ITEM_C, " +
                                        "--->tb_1_.NAME, tb_1_.FK_ORDER_X, tb_1_.FK_ORDER_Y " +
                                        "from ORDER_ITEM as tb_1_ " +
                                        "where (" +
                                        "--->tb_1_.ORDER_ITEM_A, tb_1_.ORDER_ITEM_B, tb_1_.ORDER_ITEM_C" +
                                        ") in ((?, ?, ?), (?, ?, ?)) " +
                                        "for update"
                        );
                        it.variables(1, 1, 1, 1, 1, 2);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from ORDER_ITEM " +
                                        "where (" +
                                        "--->ORDER_ITEM_A, ORDER_ITEM_B, ORDER_ITEM_C" +
                                        ") in ((?, ?, ?), (?, ?, ?))"
                        );
                        it.variables(1, 1, 1, 1, 1, 2);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from ORDER_ " +
                                        "where (ORDER_X, ORDER_Y) in ((?, ?))"
                        );
                        it.variables("001", "001");
                    });
                    ctx.rowCount(7);
                }
        );
        assertEvents(
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.embedded.OrderItem.products, " +
                        "--->sourceId={\"a\":1,\"b\":1,\"c\":1}, " +
                        "--->detachedTargetId={\"alpha\":\"00A\",\"beta\":\"00A\"}, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.embedded.Product.orderItems, " +
                        "--->sourceId={\"alpha\":\"00A\",\"beta\":\"00A\"}, " +
                        "--->detachedTargetId={\"a\":1,\"b\":1,\"c\":1}, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.embedded.OrderItem.products, " +
                        "--->sourceId={\"a\":1,\"b\":1,\"c\":1}, " +
                        "--->detachedTargetId={\"alpha\":\"00B\",\"beta\":\"00A\"}, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.embedded.Product.orderItems, " +
                        "--->sourceId={\"alpha\":\"00B\",\"beta\":\"00A\"}, " +
                        "--->detachedTargetId={\"a\":1,\"b\":1,\"c\":1}, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.embedded.OrderItem.products, " +
                        "--->sourceId={\"a\":1,\"b\":1,\"c\":2}, " +
                        "--->detachedTargetId={\"alpha\":\"00A\",\"beta\":\"00A\"}, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.embedded.Product.orderItems, " +
                        "--->sourceId={\"alpha\":\"00A\",\"beta\":\"00A\"}, " +
                        "--->detachedTargetId={\"a\":1,\"b\":1,\"c\":2}, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.embedded.OrderItem.products, " +
                        "--->sourceId={\"a\":1,\"b\":1,\"c\":2}, " +
                        "--->detachedTargetId={\"alpha\":\"00A\",\"beta\":\"00B\"}, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.embedded.Product.orderItems, " +
                        "--->sourceId={\"alpha\":\"00A\",\"beta\":\"00B\"}, " +
                        "--->detachedTargetId={\"a\":1,\"b\":1,\"c\":2}, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "Event{" +
                        "--->oldEntity={\"id\":{\"a\":1,\"b\":1,\"c\":1}," +
                        "--->--->\"name\":\"order-item-1-1\"," +
                        "--->--->\"order\":{" +
                        "--->--->--->\"id\":{\"x\":\"001\",\"y\":\"001\"}" +
                        "--->--->}" +
                        "--->}, " +
                        "--->newEntity=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.embedded.OrderItem.order, " +
                        "--->sourceId={\"a\":1,\"b\":1,\"c\":1}, " +
                        "--->detachedTargetId={\"x\":\"001\",\"y\":\"001\"}, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.embedded.Order.orderItems, " +
                        "--->sourceId={\"x\":\"001\",\"y\":\"001\"}, " +
                        "--->detachedTargetId={\"a\":1,\"b\":1,\"c\":1}, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "Event{" +
                        "--->oldEntity={\"id\":{\"a\":1,\"b\":1,\"c\":2}," +
                        "--->--->\"name\":\"order-item-1-2\"," +
                        "--->--->\"order\":{" +
                        "--->--->--->\"id\":{\"x\":\"001\",\"y\":\"001\"}" +
                        "--->--->}" +
                        "--->}, " +
                        "--->newEntity=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.embedded.OrderItem.order, " +
                        "--->sourceId={\"a\":1,\"b\":1,\"c\":2}, " +
                        "--->detachedTargetId={\"x\":\"001\",\"y\":\"001\"}, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.embedded.Order.orderItems, " +
                        "--->sourceId={\"x\":\"001\",\"y\":\"001\"}, " +
                        "--->detachedTargetId={\"a\":1,\"b\":1,\"c\":2}, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "Event{" +
                        "--->oldEntity={\"id\":{\"x\":\"001\",\"y\":\"001\"}," +
                        "--->\"name\":\"order-1\"}, " +
                        "--->newEntity=null, " +
                        "--->reason=null" +
                        "}"
        );
    }

    @Test
    public void testDeleteWithJoin() {
        OrderItemTable orderItem = OrderItemTable.$;
        executeAndExpectRowCount(
                getSqlClient()
                        .createDelete(orderItem)
                        .where(orderItem.order().name().eq("order-1")),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select " +
                                        "--->tb_1_.ORDER_ITEM_A, tb_1_.ORDER_ITEM_B, tb_1_.ORDER_ITEM_C, " +
                                        "--->tb_1_.NAME, tb_1_.FK_ORDER_X, tb_1_.FK_ORDER_Y " +
                                        "from ORDER_ITEM as tb_1_ " +
                                        "inner join ORDER_ as tb_2_ on " +
                                        "--->tb_1_.FK_ORDER_X = tb_2_.ORDER_X and " +
                                        "--->tb_1_.FK_ORDER_Y = tb_2_.ORDER_Y " +
                                        "where tb_2_.NAME = ?"
                        );
                        it.variables("order-1");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select " +
                                        "--->FK_ORDER_ITEM_A, FK_ORDER_ITEM_B, FK_ORDER_ITEM_C, " +
                                        "--->FK_PRODUCT_ALPHA, FK_PRODUCT_BETA " +
                                        "from ORDER_ITEM_PRODUCT_MAPPING " +
                                        "where (" +
                                        "--->FK_ORDER_ITEM_A, FK_ORDER_ITEM_B, FK_ORDER_ITEM_C" +
                                        ") in (" +
                                        "--->(?, ?, ?), (?, ?, ?)" +
                                        ")"
                        );
                        it.variables(1, 1, 1, 1, 1, 2);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from ORDER_ITEM_PRODUCT_MAPPING where (" +
                                        "--->FK_ORDER_ITEM_A, FK_ORDER_ITEM_B, FK_ORDER_ITEM_C, " +
                                        "--->FK_PRODUCT_ALPHA, FK_PRODUCT_BETA" +
                                        ") in (" +
                                        "--->(?, ?, ?, ?, ?), (?, ?, ?, ?, ?), (?, ?, ?, ?, ?), (?, ?, ?, ?, ?)" +
                                        ")"
                        );
                        it.variables(
                                1, 1, 1, "00A", "00A",
                                1, 1, 1, "00B", "00A",
                                1, 1, 2, "00A", "00A",
                                1, 1, 2, "00A", "00B"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from ORDER_ITEM " +
                                        "where (" +
                                        "--->ORDER_ITEM_A, ORDER_ITEM_B, ORDER_ITEM_C" +
                                        ") in ((?, ?, ?), (?, ?, ?))"
                        );
                        it.variables(1, 1, 1, 1, 1, 2);
                    });
                    ctx.rowCount(6);
                }
        );
        assertEvents(
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.embedded.OrderItem.products, " +
                        "--->sourceId={\"a\":1,\"b\":1,\"c\":1}, " +
                        "--->detachedTargetId={\"alpha\":\"00A\",\"beta\":\"00A\"}, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.embedded.Product.orderItems, " +
                        "--->sourceId={\"alpha\":\"00A\",\"beta\":\"00A\"}, " +
                        "--->detachedTargetId={\"a\":1,\"b\":1,\"c\":1}, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.embedded.OrderItem.products, " +
                        "--->sourceId={\"a\":1,\"b\":1,\"c\":1}, " +
                        "--->detachedTargetId={\"alpha\":\"00B\",\"beta\":\"00A\"}, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.embedded.Product.orderItems, " +
                        "--->sourceId={\"alpha\":\"00B\",\"beta\":\"00A\"}, " +
                        "--->detachedTargetId={\"a\":1,\"b\":1,\"c\":1}, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.embedded.OrderItem.products, " +
                        "--->sourceId={\"a\":1,\"b\":1,\"c\":2}, " +
                        "--->detachedTargetId={\"alpha\":\"00A\",\"beta\":\"00A\"}, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.embedded.Product.orderItems, " +
                        "--->sourceId={\"alpha\":\"00A\",\"beta\":\"00A\"}, " +
                        "--->detachedTargetId={\"a\":1,\"b\":1,\"c\":2}, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.embedded.OrderItem.products, " +
                        "--->sourceId={\"a\":1,\"b\":1,\"c\":2}, " +
                        "--->detachedTargetId={\"alpha\":\"00A\",\"beta\":\"00B\"}, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.embedded.Product.orderItems, " +
                        "--->sourceId={\"alpha\":\"00A\",\"beta\":\"00B\"}, " +
                        "--->detachedTargetId={\"a\":1,\"b\":1,\"c\":2}, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "Event{" +
                        "--->oldEntity={" +
                        "--->--->\"id\":{\"a\":1,\"b\":1,\"c\":1}," +
                        "--->--->\"name\":\"order-item-1-1\"," +
                        "--->--->\"order\":{" +
                        "--->--->--->\"id\":{\"x\":\"001\",\"y\":\"001\"}" +
                        "--->--->}" +
                        "--->}, " +
                        "--->newEntity=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.embedded.OrderItem.order, " +
                        "--->sourceId={\"a\":1,\"b\":1,\"c\":1}, " +
                        "--->detachedTargetId={\"x\":\"001\",\"y\":\"001\"}, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.embedded.Order.orderItems, " +
                        "--->sourceId={\"x\":\"001\",\"y\":\"001\"}, " +
                        "--->detachedTargetId={\"a\":1,\"b\":1,\"c\":1}, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "Event{" +
                        "--->oldEntity={" +
                        "--->--->\"id\":{\"a\":1,\"b\":1,\"c\":2}," +
                        "--->--->\"name\":\"order-item-1-2\"," +
                        "--->--->\"order\":{" +
                        "--->--->--->\"id\":{\"x\":\"001\",\"y\":\"001\"}" +
                        "--->--->}" +
                        "--->}, " +
                        "--->newEntity=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.embedded.OrderItem.order, " +
                        "--->sourceId={\"a\":1,\"b\":1,\"c\":2}, " +
                        "--->detachedTargetId={\"x\":\"001\",\"y\":\"001\"}, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.embedded.Order.orderItems, " +
                        "--->sourceId={\"x\":\"001\",\"y\":\"001\"}, " +
                        "--->detachedTargetId={\"a\":1,\"b\":1,\"c\":2}, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}"
        );
    }
}
