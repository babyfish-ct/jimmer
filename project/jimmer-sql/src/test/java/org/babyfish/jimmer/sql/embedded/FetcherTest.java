package org.babyfish.jimmer.sql.embedded;

import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.embedded.*;
import org.junit.jupiter.api.Test;

public class FetcherTest extends AbstractQueryTest {

    @Test
    public void testFromOrderToProduct() {
        OrderTable order = OrderTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(order)
                        .select(
                                order.fetch(
                                        OrderFetcher.$
                                                .allScalarFields()
                                                .orderItems(
                                                        OrderItemFetcher.$
                                                                .allScalarFields()
                                                                .products(
                                                                        ProductFetcher.$
                                                                                .allScalarFields()
                                                                )
                                                )
                                )
                        ),
                ctx -> {
                    ctx.sql("select tb_1_.ORDER_X, tb_1_.ORDER_Y, tb_1_.NAME from ORDER_ as tb_1_");
                    ctx.statement(1).sql(
                            "select " +
                                    "--->tb_1_.FK_ORDER_X, tb_1_.FK_ORDER_Y, " +
                                    "--->tb_1_.ORDER_ITEM_A, tb_1_.ORDER_ITEM_B, tb_1_.ORDER_ITEM_C, " +
                                    "--->tb_1_.NAME " +
                                    "from ORDER_ITEM as tb_1_ " +
                                    "where (" +
                                    "--->tb_1_.FK_ORDER_X, tb_1_.FK_ORDER_Y" +
                                    "--->) in (" +
                                    "--->(?, ?), (?, ?)" +
                                    ")"
                    );
                    ctx.statement(2).sql(
                            "select " +
                                    "--->tb_2_.FK_ORDER_ITEM_A, tb_2_.FK_ORDER_ITEM_B, tb_2_.FK_ORDER_ITEM_C, " +
                                    "--->tb_1_.PRODUCT_ALPHA, tb_1_.PRODUCT_BETA, " +
                                    "--->tb_1_.NAME " +
                                    "from PRODUCT as tb_1_ " +
                                    "inner join ORDER_ITEM_PRODUCT_MAPPING as tb_2_ on " +
                                    "--->tb_1_.PRODUCT_ALPHA = tb_2_.FK_PRODUCT_ALPHA and " +
                                    "--->tb_1_.PRODUCT_BETA = tb_2_.FK_PRODUCT_BETA " +
                                    "where (" +
                                    "--->tb_2_.FK_ORDER_ITEM_A, tb_2_.FK_ORDER_ITEM_B, tb_2_.FK_ORDER_ITEM_C" +
                                    ") in (" +
                                    "--->(?, ?, ?), (?, ?, ?), (?, ?, ?), (?, ?, ?)" +
                                    ")"
                    );
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":{\"x\":\"001\",\"y\":\"001\"}," +
                                    "--->--->\"name\":\"order-1\"," +
                                    "--->--->\"orderItems\":[" +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":{\"a\":1,\"b\":1,\"c\":1}," +
                                    "--->--->--->--->\"name\":\"order-item-1-1\"," +
                                    "--->--->--->--->\"products\":[" +
                                    "--->--->--->--->--->{" +
                                    "--->--->--->--->--->--->\"id\":{\"alpha\":\"00A\",\"beta\":\"00A\"}," +
                                    "--->--->--->--->--->--->\"name\":\"Car\"" +
                                    "--->--->--->--->--->},{" +
                                    "--->--->--->--->--->--->\"id\":{\"alpha\":\"00B\",\"beta\":\"00A\"}," +
                                    "--->--->--->--->--->--->\"name\":\"Bike\"" +
                                    "--->--->--->--->--->}" +
                                    "--->--->--->--->]" +
                                    "--->--->--->},{" +
                                    "--->--->--->--->\"id\":{\"a\":1,\"b\":1,\"c\":2}," +
                                    "--->--->--->--->\"name\":\"order-item-1-2\"," +
                                    "--->--->--->--->\"products\":[" +
                                    "--->--->--->--->--->{" +
                                    "--->--->--->--->--->--->\"id\":{\"alpha\":\"00A\",\"beta\":\"00A\"}," +
                                    "--->--->--->--->--->--->\"name\":\"Car\"" +
                                    "--->--->--->--->--->},{" +
                                    "--->--->--->--->--->--->\"id\":{\"alpha\":\"00A\",\"beta\":\"00B\"}," +
                                    "--->--->--->--->--->--->\"name\":\"Boat\"" +
                                    "--->--->--->--->--->}" +
                                    "--->--->--->--->]" +
                                    "--->--->--->}" +
                                    "--->--->]" +
                                    "--->},{" +
                                    "--->--->\"id\":{\"x\":\"001\",\"y\":\"002\"}," +
                                    "--->--->\"name\":\"order-2\"," +
                                    "--->--->\"orderItems\":[" +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":{\"a\":1,\"b\":2,\"c\":1}," +
                                    "--->--->--->--->\"name\":\"order-item-2-1\"," +
                                    "--->--->--->--->\"products\":[" +
                                    "--->--->--->--->--->{" +
                                    "--->--->--->--->--->--->\"id\":{\"alpha\":\"00A\",\"beta\":\"00B\"}," +
                                    "--->--->--->--->--->--->\"name\":\"Boat\"" +
                                    "--->--->--->--->--->},{" +
                                    "--->--->--->--->--->--->\"id\":{\"alpha\":\"00B\",\"beta\":\"00A\"}," +
                                    "--->--->--->--->--->--->\"name\":\"Bike\"" +
                                    "--->--->--->--->--->}" +
                                    "--->--->--->--->]" +
                                    "--->--->--->},{" +
                                    "--->--->--->--->\"id\":{\"a\":2,\"b\":1,\"c\":1}," +
                                    "--->--->--->--->\"name\":\"order-item-2-2\"," +
                                    "--->--->--->--->\"products\":[" +
                                    "--->--->--->--->--->{" +
                                    "--->--->--->--->--->--->\"id\":{\"alpha\":\"00A\",\"beta\":\"00B\"}," +
                                    "--->--->--->--->--->--->\"name\":\"Boat\"" +
                                    "--->--->--->--->--->},{" +
                                    "--->--->--->--->--->--->\"id\":{\"alpha\":\"00B\",\"beta\":\"00A\"}," +
                                    "--->--->--->--->--->--->\"name\":\"Bike\"" +
                                    "--->--->--->--->--->}" +
                                    "--->--->--->--->]" +
                                    "--->--->--->}" +
                                    "--->--->]" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testFromProductToOrder() {
        ProductTable product = ProductTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(product)
                        .select(
                                product.fetch(
                                        ProductFetcher.$
                                                .allScalarFields()
                                                .orderItems(
                                                        OrderItemFetcher.$
                                                                .allScalarFields()
                                                                .order(
                                                                        OrderFetcher.$
                                                                                .allScalarFields()
                                                                )
                                                )
                                )
                        ),
                ctx -> {
                    ctx.sql("select tb_1_.PRODUCT_ALPHA, tb_1_.PRODUCT_BETA, tb_1_.NAME from PRODUCT as tb_1_");
                    ctx.statement(1).sql(
                            "select " +
                                    "--->tb_2_.FK_PRODUCT_ALPHA, tb_2_.FK_PRODUCT_BETA, " +
                                    "--->tb_1_.ORDER_ITEM_A, tb_1_.ORDER_ITEM_B, tb_1_.ORDER_ITEM_C, " +
                                    "--->tb_1_.NAME, tb_1_.FK_ORDER_X, tb_1_.FK_ORDER_Y " +
                                    "from ORDER_ITEM as tb_1_ " +
                                    "inner join ORDER_ITEM_PRODUCT_MAPPING as tb_2_ on " +
                                    "--->tb_1_.ORDER_ITEM_A = tb_2_.FK_ORDER_ITEM_A and " +
                                    "--->tb_1_.ORDER_ITEM_B = tb_2_.FK_ORDER_ITEM_B and " +
                                    "--->tb_1_.ORDER_ITEM_C = tb_2_.FK_ORDER_ITEM_C " +
                                    "where (" +
                                    "--->tb_2_.FK_PRODUCT_ALPHA, tb_2_.FK_PRODUCT_BETA" +
                                    ") in (" +
                                    "--->(?, ?), (?, ?), (?, ?)" +
                                    ")"
                    );
                    ctx.statement(2).sql(
                            "select tb_1_.ORDER_X, tb_1_.ORDER_Y, tb_1_.NAME " +
                                    "from ORDER_ as tb_1_ " +
                                    "where (tb_1_.ORDER_X, tb_1_.ORDER_Y) in ((?, ?), (?, ?))"
                    );
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":{\"alpha\":\"00A\",\"beta\":\"00A\"}," +
                                    "--->--->\"name\":\"Car\"," +
                                    "--->--->\"orderItems\":[" +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":{\"a\":1,\"b\":1,\"c\":1}," +
                                    "--->--->--->--->\"name\":\"order-item-1-1\"," +
                                    "--->--->--->--->\"order\":{" +
                                    "--->--->--->--->--->\"id\":{\"x\":\"001\",\"y\":\"001\"}," +
                                    "--->--->--->--->--->\"name\":\"order-1\"" +
                                    "--->--->--->--->}" +
                                    "--->--->--->},{" +
                                    "--->--->--->--->\"id\":{\"a\":1,\"b\":1,\"c\":2}," +
                                    "--->--->--->--->\"name\":\"order-item-1-2\"," +
                                    "--->--->--->--->\"order\":{" +
                                    "--->--->--->--->--->\"id\":{\"x\":\"001\",\"y\":\"001\"}," +
                                    "--->--->--->--->--->\"name\":\"order-1\"" +
                                    "--->--->--->--->}" +
                                    "--->--->--->}" +
                                    "--->--->]" +
                                    "--->},{" +
                                    "--->--->\"id\":{\"alpha\":\"00A\",\"beta\":\"00B\"}," +
                                    "--->--->\"name\":\"Boat\"," +
                                    "--->--->\"orderItems\":[" +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":{\"a\":1,\"b\":1,\"c\":2}," +
                                    "--->--->--->--->\"name\":\"order-item-1-2\"," +
                                    "--->--->--->--->\"order\":{" +
                                    "--->--->--->--->--->\"id\":{\"x\":\"001\",\"y\":\"001\"}," +
                                    "--->--->--->--->--->\"name\":\"order-1\"" +
                                    "--->--->--->--->}" +
                                    "--->--->--->},{" +
                                    "--->--->--->--->\"id\":{\"a\":1,\"b\":2,\"c\":1}," +
                                    "--->--->--->--->\"name\":\"order-item-2-1\"," +
                                    "--->--->--->--->\"order\":{" +
                                    "--->--->--->--->--->\"id\":{\"x\":\"001\",\"y\":\"002\"}," +
                                    "--->--->--->--->--->\"name\":\"order-2\"" +
                                    "--->--->--->--->}" +
                                    "--->--->--->},{" +
                                    "--->--->--->--->\"id\":{\"a\":2,\"b\":1,\"c\":1}," +
                                    "--->--->--->--->\"name\":\"order-item-2-2\"," +
                                    "--->--->--->--->\"order\":{" +
                                    "--->--->--->--->--->\"id\":{\"x\":\"001\",\"y\":\"002\"}," +
                                    "--->--->--->--->--->\"name\":\"order-2\"" +
                                    "--->--->--->--->}" +
                                    "--->--->--->}" +
                                    "--->--->]" +
                                    "--->},{" +
                                    "--->--->\"id\":{\"alpha\":\"00B\",\"beta\":\"00A\"}," +
                                    "--->--->\"name\":\"Bike\"," +
                                    "--->--->\"orderItems\":[" +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":{\"a\":1,\"b\":1,\"c\":1}," +
                                    "--->--->--->--->\"name\":\"order-item-1-1\"," +
                                    "--->--->--->--->\"order\":{" +
                                    "--->--->--->--->--->\"id\":{\"x\":\"001\",\"y\":\"001\"}," +
                                    "--->--->--->--->--->\"name\":\"order-1\"" +
                                    "--->--->--->--->}" +
                                    "--->--->--->},{" +
                                    "--->--->--->--->\"id\":{\"a\":1,\"b\":2,\"c\":1}," +
                                    "--->--->--->--->\"name\":\"order-item-2-1\"," +
                                    "--->--->--->--->\"order\":{" +
                                    "--->--->--->--->--->\"id\":{\"x\":\"001\",\"y\":\"002\"}," +
                                    "--->--->--->--->--->\"name\":\"order-2\"" +
                                    "--->--->--->--->}" +
                                    "--->--->--->},{" +
                                    "--->--->--->--->\"id\":{\"a\":2,\"b\":1,\"c\":1}," +
                                    "--->--->--->--->\"name\":\"order-item-2-2\"," +
                                    "--->--->--->--->\"order\":{" +
                                    "--->--->--->--->--->\"id\":{\"x\":\"001\",\"y\":\"002\"}," +
                                    "--->--->--->--->--->\"name\":\"order-2\"" +
                                    "--->--->--->--->}" +
                                    "--->--->--->}" +
                                    "--->--->]" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void fetchWithNullReference() {
        OrderItemTable orderItem = OrderItemTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(orderItem)
                        .select(
                                orderItem.fetch(
                                        OrderItemFetcher.$
                                                .allScalarFields()
                                                .order(
                                                        OrderFetcher.$.allScalarFields()
                                                )
                                )
                        ),
                ctx ->{
                    ctx.sql(
                            "select " +
                                    "--->tb_1_.ORDER_ITEM_A, tb_1_.ORDER_ITEM_B, tb_1_.ORDER_ITEM_C, " +
                                    "--->tb_1_.NAME, tb_1_.FK_ORDER_X, tb_1_.FK_ORDER_Y " +
                                    "from ORDER_ITEM as tb_1_"
                    );
                    ctx.statement(1).sql(
                            "select tb_1_.ORDER_X, tb_1_.ORDER_Y, tb_1_.NAME " +
                                    "from ORDER_ as tb_1_ " +
                                    "where (tb_1_.ORDER_X, tb_1_.ORDER_Y) in ((?, ?), (?, ?))"
                    );
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":{\"a\":1,\"b\":1,\"c\":1}," +
                                    "--->--->\"name\":\"order-item-1-1\"," +
                                    "--->--->\"order\":{" +
                                    "--->--->--->\"id\":{\"x\":\"001\",\"y\":\"001\"}," +
                                    "--->--->--->\"name\":\"order-1\"" +
                                    "--->--->}" +
                                    "--->},{" +
                                    "--->--->\"id\":{\"a\":1,\"b\":1,\"c\":2}," +
                                    "--->--->\"name\":\"order-item-1-2\"," +
                                    "--->--->\"order\":{" +
                                    "--->--->--->\"id\":{\"x\":\"001\",\"y\":\"001\"}," +
                                    "--->--->--->\"name\":\"order-1\"" +
                                    "--->--->}" +
                                    "--->},{" +
                                    "--->--->\"id\":{\"a\":1,\"b\":2,\"c\":1}," +
                                    "--->--->\"name\":\"order-item-2-1\"," +
                                    "--->--->\"order\":{" +
                                    "--->--->--->\"id\":{\"x\":\"001\",\"y\":\"002\"}," +
                                    "--->--->--->\"name\":\"order-2\"" +
                                    "--->--->}" +
                                    "--->},{" +
                                    "--->--->\"id\":{\"a\":2,\"b\":1,\"c\":1}," +
                                    "--->--->\"name\":\"order-item-2-2\"," +
                                    "--->--->\"order\":{" +
                                    "--->--->--->\"id\":{\"x\":\"001\",\"y\":\"002\"}," +
                                    "--->--->--->\"name\":\"order-2\"" +
                                    "--->--->}" +
                                    "--->},{" +
                                    "--->--->\"id\":{\"a\":9,\"b\":9,\"c\":9}," +
                                    "--->--->\"name\":\"order-item-X-X\"," +
                                    "--->--->\"order\":null" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }
}
