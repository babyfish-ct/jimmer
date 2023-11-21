package org.babyfish.jimmer.sql.embedded;

import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.embedded.OrderItemTable;
import org.junit.jupiter.api.Test;

public class CountTest extends AbstractQueryTest {

    @Test
    public void testCount() {

        OrderItemTable table = OrderItemTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .select(table.orderId().count()),
                ctx -> {
                    ctx.sql("select count((tb_1_.FK_ORDER_X, tb_1_.FK_ORDER_Y)) from ORDER_ITEM tb_1_");
                    ctx.rows("[5]");
                }
        );
    }
}
