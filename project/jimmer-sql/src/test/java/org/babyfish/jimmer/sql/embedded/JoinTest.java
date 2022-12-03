package org.babyfish.jimmer.sql.embedded;

import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.embedded.*;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

public class JoinTest extends AbstractQueryTest {

    @Test
    public void testFromOrderToProduct() {
        OrderTable order = OrderTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(order)
                        .where(order.asTableEx().orderItems().name().eq("ORDER_ITEM"))
                        .where(order.asTableEx().orderItems().products().name().eq("PRODUCT"))
                        .select(order),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ORDER_X, tb_1_.ORDER_Y, tb_1_.NAME " +
                                    "from ORDER_ as tb_1_ " +
                                    "inner join ORDER_ITEM as tb_2_ on " +
                                    "--->tb_1_.ORDER_X = tb_2_.FK_ORDER_X and " +
                                    "--->tb_1_.ORDER_Y = tb_2_.FK_ORDER_Y " +
                                    "inner join ORDER_ITEM_PRODUCT_MAPPING as tb_3_ on " +
                                    "--->tb_2_.ORDER_ITEM_A = tb_3_.FK_ORDER_ITEM_A and " +
                                    "--->tb_2_.ORDER_ITEM_B = tb_3_.FK_ORDER_ITEM_B and " +
                                    "--->tb_2_.ORDER_ITEM_C = tb_3_.FK_ORDER_ITEM_C " +
                                    "inner join PRODUCT as tb_4_ on " +
                                    "--->tb_3_.FK_PRODUCT_ALPHA = tb_4_.PRODUCT_ALPHA and " +
                                    "--->tb_3_.FK_PRODUCT_BETA = tb_4_.PRODUCT_BETA " +
                                    "where tb_2_.NAME = ? and tb_4_.NAME = ?"
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
                        .where(product.asTableEx().orderItems().name().eq("ORDER_ITEM"))
                        .where(product.asTableEx().orderItems().order().name().eq("ORDER"))
                        .select(product),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.PRODUCT_ALPHA, tb_1_.PRODUCT_BETA, tb_1_.NAME from PRODUCT as tb_1_ " +
                                    "inner join ORDER_ITEM_PRODUCT_MAPPING as tb_2_ on " +
                                    "--->tb_1_.PRODUCT_ALPHA = tb_2_.FK_PRODUCT_ALPHA and " +
                                    "--->tb_1_.PRODUCT_BETA = tb_2_.FK_PRODUCT_BETA " +
                                    "inner join ORDER_ITEM as tb_3_ on " +
                                    "--->tb_2_.FK_ORDER_ITEM_A = tb_3_.ORDER_ITEM_A and " +
                                    "--->tb_2_.FK_ORDER_ITEM_B = tb_3_.ORDER_ITEM_B and " +
                                    "--->tb_2_.FK_ORDER_ITEM_C = tb_3_.ORDER_ITEM_C " +
                                    "inner join ORDER_ as tb_4_ on " +
                                    "--->tb_3_.FK_ORDER_X = tb_4_.ORDER_X and " +
                                    "--->tb_3_.FK_ORDER_Y = tb_4_.ORDER_Y " +
                                    "where tb_3_.NAME = ? and tb_4_.NAME = ?"
                    );
                }
        );
    }

    @Test
    public void testPhantomJoin() {
        OrderItemTable orderItem = OrderItemTable.$;
        List<OrderId> parentIds = Arrays.asList(
                OrderIdDraft.$.produce(draft -> draft.setX("001").setY("001")),
                OrderIdDraft.$.produce(draft -> draft.setX("001").setY("002"))
        );
        executeAndExpect(
                getSqlClient()
                        .createQuery(orderItem)
                        .where(orderItem.order().id().in(parentIds))
                        .select(orderItem),
                ctx -> {
                    ctx.sql(
                            "select " +
                                    "--->tb_1_.ORDER_ITEM_A, tb_1_.ORDER_ITEM_B, tb_1_.ORDER_ITEM_C, " +
                                    "--->tb_1_.NAME, tb_1_.FK_ORDER_X, tb_1_.FK_ORDER_Y " +
                                    "from ORDER_ITEM as tb_1_ " +
                                    "where (tb_1_.FK_ORDER_X, tb_1_.FK_ORDER_Y) in ((?, ?), (?, ?))"
                    );
                }
        );
    }

    @Test
    public void testPhantomJoinByPartialId() {
        OrderItemTable orderItem = OrderItemTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(orderItem)
                        .where(orderItem.order().id().x().in(Arrays.asList("001", "002")))
                        .select(orderItem),
                ctx -> {
                    ctx.sql(
                            "select " +
                                    "--->tb_1_.ORDER_ITEM_A, tb_1_.ORDER_ITEM_B, tb_1_.ORDER_ITEM_C, " +
                                    "--->tb_1_.NAME, tb_1_.FK_ORDER_X, tb_1_.FK_ORDER_Y " +
                                    "from ORDER_ITEM as tb_1_ " +
                                    "where tb_1_.FK_ORDER_X in (?, ?)"
                    );
                }
        );
    }

    @Test
    public void testHalfJoin() {
        OrderItemTable orderItem = OrderItemTable.$;
        List<ProductId> productIds = Arrays.asList(
                ProductIdDraft.$.produce(draft -> draft.setAlpha("00A").setBeta("00A")),
                ProductIdDraft.$.produce(draft -> draft.setAlpha("00A").setBeta("00B"))
        );
        executeAndExpect(
                getSqlClient()
                        .createQuery(orderItem)
                        .where(orderItem.asTableEx().products().id().in(productIds))
                        .select(orderItem),
                ctx -> {
                    ctx.sql(
                            "select " +
                                    "--->tb_1_.ORDER_ITEM_A, tb_1_.ORDER_ITEM_B, tb_1_.ORDER_ITEM_C, " +
                                    "--->tb_1_.NAME, tb_1_.FK_ORDER_X, tb_1_.FK_ORDER_Y " +
                                    "from ORDER_ITEM as tb_1_ " +
                                    "inner join ORDER_ITEM_PRODUCT_MAPPING as tb_2_ on " +
                                    "--->tb_1_.ORDER_ITEM_A = tb_2_.FK_ORDER_ITEM_A and " +
                                    "--->tb_1_.ORDER_ITEM_B = tb_2_.FK_ORDER_ITEM_B and " +
                                    "--->tb_1_.ORDER_ITEM_C = tb_2_.FK_ORDER_ITEM_C " +
                                    "where (tb_2_.FK_PRODUCT_ALPHA, tb_2_.FK_PRODUCT_BETA) in ((?, ?), (?, ?))"
                    );
                }
        );
    }

    @Test
    public void testHalfJoinByPartialId() {
        OrderItemTable orderItem = OrderItemTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(orderItem)
                        .where(orderItem.asTableEx().products().id().alpha().in(Arrays.asList("00A", "00B")))
                        .select(orderItem),
                ctx -> {
                    ctx.sql(
                            "select " +
                                    "--->tb_1_.ORDER_ITEM_A, tb_1_.ORDER_ITEM_B, tb_1_.ORDER_ITEM_C, " +
                                    "--->tb_1_.NAME, tb_1_.FK_ORDER_X, tb_1_.FK_ORDER_Y " +
                                    "from ORDER_ITEM as tb_1_ " +
                                    "inner join ORDER_ITEM_PRODUCT_MAPPING as tb_2_ on " +
                                    "--->tb_1_.ORDER_ITEM_A = tb_2_.FK_ORDER_ITEM_A and " +
                                    "--->tb_1_.ORDER_ITEM_B = tb_2_.FK_ORDER_ITEM_B and " +
                                    "--->tb_1_.ORDER_ITEM_C = tb_2_.FK_ORDER_ITEM_C " +
                                    "where tb_2_.FK_PRODUCT_ALPHA in (?, ?)"
                    );
                }
        );
    }
}
