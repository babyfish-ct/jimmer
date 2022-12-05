package org.babyfish.jimmer.sql.embedded;

import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.model.embedded.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class StatementTest extends AbstractMutationTest {

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
                                "update TRANSFORM tb_1_ " +
                                        "set TARGET_BOTTOM = tb_1_.TARGET_BOTTOM + ? " +
                                        "where tb_1_.ID = ?"
                        );
                        it.variables(100L, 1L);
                    });
                    ctx.rowCount(1);
                }
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
                                "update ORDER_ITEM tb_1_ " +
                                        "set NAME = upper(tb_1_.NAME) " +
                                        "where (" +
                                        "--->tb_1_.ORDER_ITEM_A, tb_1_.ORDER_ITEM_B, tb_1_.ORDER_ITEM_C" +
                                        ") = (" +
                                        "--->?, ?, ?" +
                                        ")");
                        it.variables(1, 1, 1);
                    });
                    ctx.rowCount(1);
                }
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
                                "delete from ORDER_ as tb_1_ " +
                                        "where (tb_1_.ORDER_X, tb_1_.ORDER_Y) = (?, ?)"
                        );
                        it.variables("001", "001");
                    });
                    ctx.rowCount(1);
                }
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
                                "select distinct tb_1_.ORDER_ITEM_A, tb_1_.ORDER_ITEM_B, tb_1_.ORDER_ITEM_C " +
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
                                "delete from ORDER_ITEM_PRODUCT_MAPPING " +
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
                                "delete from ORDER_ITEM " +
                                        "where (" +
                                        "--->ORDER_ITEM_A, ORDER_ITEM_B, ORDER_ITEM_C" +
                                        ") in (" +
                                        "--->(?, ?, ?), (?, ?, ?)" +
                                        ")"
                        );
                        it.variables(1, 1, 1, 1, 1, 2);
                    });
                    ctx.rowCount(6);
                }
        );
    }
}
