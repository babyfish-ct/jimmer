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
                                "update TRANSFORM tb_1_ set tb_1_.TARGET_BOTTOM = tb_1_.TARGET_BOTTOM + ? " +
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
                                        "set tb_1_.NAME = upper(tb_1_.NAME) " +
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
}
