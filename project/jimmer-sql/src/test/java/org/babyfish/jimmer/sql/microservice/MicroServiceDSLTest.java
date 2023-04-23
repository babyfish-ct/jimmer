package org.babyfish.jimmer.sql.microservice;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.microservice.OrderItemTable;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class MicroServiceDSLTest extends AbstractQueryTest {

    @Test
    public void testFilterByManyToOne() {
        OrderItemTable table = OrderItemTable.$;
        JSqlClient sqlClient = getSqlClient(builder ->
                builder
                        .setMicroServiceName("order-item-service")
                        .setMicroServiceExchange(new MicroServiceExchangeImpl())
        );
        executeAndExpect(
                sqlClient
                        .createQuery(table)
                        .where(table.order().id().eq(1L))
                        .select(table),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.ORDER_ID " +
                                    "from MS_ORDER_ITEM tb_1_ " +
                                    "where tb_1_.ORDER_ID = ?"
                    );
                    ctx.rows(
                            "[" +
                                    "--->{\"id\":1,\"name\":\"ms-order-1.item-1\",\"order\":{\"id\":1}}," +
                                    "--->{\"id\":2,\"name\":\"ms-order-1.item-2\",\"order\":{\"id\":1}}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testFilterByManyToMany() {
        OrderItemTable table = OrderItemTable.$;
        JSqlClient sqlClient = getSqlClient(builder ->
                builder
                        .setMicroServiceName("order-item-service")
                        .setMicroServiceExchange(new MicroServiceExchangeImpl())
        );
        executeAndExpect(
                sqlClient
                        .createQuery(table)
                        .where(table.asTableEx().products().id().eq(2L))
                        .select(table.id())
                        .distinct(),
                ctx -> {
                    ctx.sql(
                            "select distinct tb_1_.ID " +
                                    "from MS_ORDER_ITEM tb_1_ " +
                                    "inner join MS_ORDER_ITEM_PRODUCT_MAPPING tb_2_ " +
                                    "--->on tb_1_.ID = tb_2_.ORDER_ITEM_ID " +
                                    "where tb_2_.PRODUCT_ID = ?"
                    );
                    ctx.rows("[1,2,4]");
                }
        );
    }
}
