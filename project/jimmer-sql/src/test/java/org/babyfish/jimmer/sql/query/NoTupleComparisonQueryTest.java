package org.babyfish.jimmer.sql.query;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.dialect.H2Dialect;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.model.Fetchers;
import org.babyfish.jimmer.sql.model.Immutables;
import org.babyfish.jimmer.sql.model.embedded.*;
import org.junit.jupiter.api.Test;

import java.util.Collections;

public class NoTupleComparisonQueryTest extends AbstractQueryTest {

    private static final Fetcher<Order> ORDER_FETCHER = OrderFetcher.$
            .name()
            .orderItems(
                    OrderItemFetcher.$
                            .name()
                            .products(
                                    ProductFetcher.$
                                            .name()
                            )
            );

    private final JSqlClient sqlClient =
            getSqlClient(it -> {
                it.setDialect(new H2Dialect() {
                    @Override
                    public boolean isTupleComparisonSupported() {
                        return false;
                    }
                });
            });

    @Test
    public void testEq() {
        OrderTable table = OrderTable.$;
        executeAndExpect(
                sqlClient.createQuery(table)
                        .where(table.id().eq(Immutables.createOrderId(id -> id.setX("001").setY("002"))))
                        .select(
                                table.fetch(ORDER_FETCHER)
                        ),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ORDER_X, tb_1_.ORDER_Y, tb_1_.NAME " +
                                    "from ORDER_ tb_1_ " +
                                    "where tb_1_.ORDER_X = ? and tb_1_.ORDER_Y = ?"
                    );
                    ctx.statement(1).sql(
                            "select tb_1_.ORDER_ITEM_A, tb_1_.ORDER_ITEM_B, tb_1_.ORDER_ITEM_C, tb_1_.NAME " +
                                    "from ORDER_ITEM tb_1_ " +
                                    "where tb_1_.FK_ORDER_X = ? and tb_1_.FK_ORDER_Y = ?"
                    );
                    ctx.statement(2).sql(
                            "select " +
                                    "--->tb_2_.FK_ORDER_ITEM_A, tb_2_.FK_ORDER_ITEM_B, tb_2_.FK_ORDER_ITEM_C, " +
                                    "--->tb_1_.PRODUCT_ALPHA, tb_1_.PRODUCT_BETA, " +
                                    "--->tb_1_.NAME " +
                                    "from PRODUCT tb_1_ " +
                                    "inner join ORDER_ITEM_PRODUCT_MAPPING tb_2_ on " +
                                    "--->tb_1_.PRODUCT_ALPHA = tb_2_.FK_PRODUCT_ALPHA and " +
                                    "--->tb_1_.PRODUCT_BETA = tb_2_.FK_PRODUCT_BETA " +
                                    "where (tb_2_.FK_ORDER_ITEM_A, tb_2_.FK_ORDER_ITEM_B, tb_2_.FK_ORDER_ITEM_C) in (" +
                                    "--->(?, ?, ?), (?, ?, ?)" +
                                    ")"
                    );
                }
        );
    }

    @Test
    public void testNe() {
        OrderTable table = OrderTable.$;
        executeAndExpect(
                sqlClient.createQuery(table)
                        .where(table.id().ne(Immutables.createOrderId(id -> id.setX("001").setY("002"))))
                        .select(
                                table.fetch(ORDER_FETCHER)
                        ),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ORDER_X, tb_1_.ORDER_Y, tb_1_.NAME " +
                                    "from ORDER_ tb_1_ " +
                                    "where (tb_1_.ORDER_X <> ? or tb_1_.ORDER_Y <> ?)"
                    );
                    ctx.statement(1).sql(
                            "select tb_1_.ORDER_ITEM_A, tb_1_.ORDER_ITEM_B, tb_1_.ORDER_ITEM_C, tb_1_.NAME " +
                                    "from ORDER_ITEM tb_1_ " +
                                    "where tb_1_.FK_ORDER_X = ? and tb_1_.FK_ORDER_Y = ?"
                    );
                    ctx.statement(2).sql(
                            "select " +
                                    "--->tb_2_.FK_ORDER_ITEM_A, tb_2_.FK_ORDER_ITEM_B, tb_2_.FK_ORDER_ITEM_C, " +
                                    "--->tb_1_.PRODUCT_ALPHA, tb_1_.PRODUCT_BETA, " +
                                    "--->tb_1_.NAME " +
                                    "from PRODUCT tb_1_ " +
                                    "inner join ORDER_ITEM_PRODUCT_MAPPING tb_2_ on " +
                                    "--->tb_1_.PRODUCT_ALPHA = tb_2_.FK_PRODUCT_ALPHA and " +
                                    "--->tb_1_.PRODUCT_BETA = tb_2_.FK_PRODUCT_BETA " +
                                    "where (tb_2_.FK_ORDER_ITEM_A, tb_2_.FK_ORDER_ITEM_B, tb_2_.FK_ORDER_ITEM_C) in (" +
                                    "--->(?, ?, ?), (?, ?, ?)" +
                                    ")"
                    );
                }
        );
    }

    @Test
    public void testIn() {
        OrderTable table = OrderTable.$;
        executeAndExpect(
                sqlClient.createQuery(table)
                        .where(table.id().in(
                                Collections.singleton(
                                        Immutables.createOrderId(id -> id.setX("001").setY("002")))
                                )
                        )
                        .select(
                                table.fetch(ORDER_FETCHER)
                        ),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ORDER_X, tb_1_.ORDER_Y, tb_1_.NAME " +
                                    "from ORDER_ tb_1_ " +
                                    "where tb_1_.ORDER_X = ? and tb_1_.ORDER_Y = ?"
                    );
                    ctx.statement(1).sql(
                            "select tb_1_.ORDER_ITEM_A, tb_1_.ORDER_ITEM_B, tb_1_.ORDER_ITEM_C, tb_1_.NAME " +
                                    "from ORDER_ITEM tb_1_ " +
                                    "where tb_1_.FK_ORDER_X = ? and tb_1_.FK_ORDER_Y = ?"
                    );
                    ctx.statement(2).sql(
                            "select " +
                                    "--->tb_2_.FK_ORDER_ITEM_A, tb_2_.FK_ORDER_ITEM_B, tb_2_.FK_ORDER_ITEM_C, " +
                                    "--->tb_1_.PRODUCT_ALPHA, tb_1_.PRODUCT_BETA, " +
                                    "--->tb_1_.NAME " +
                                    "from PRODUCT tb_1_ " +
                                    "inner join ORDER_ITEM_PRODUCT_MAPPING tb_2_ on " +
                                    "--->tb_1_.PRODUCT_ALPHA = tb_2_.FK_PRODUCT_ALPHA and " +
                                    "--->tb_1_.PRODUCT_BETA = tb_2_.FK_PRODUCT_BETA " +
                                    "where (tb_2_.FK_ORDER_ITEM_A, tb_2_.FK_ORDER_ITEM_B, tb_2_.FK_ORDER_ITEM_C) in (" +
                                    "--->(?, ?, ?), (?, ?, ?)" +
                                    ")"
                    );
                }
        );
    }

    @Test
    public void testNotIn() {
        OrderTable table = OrderTable.$;
        executeAndExpect(
                sqlClient.createQuery(table)
                        .where(table.id().notIn(
                                        Collections.singleton(
                                                Immutables.createOrderId(id -> id.setX("001").setY("002")))
                                )
                        )
                        .select(
                                table.fetch(ORDER_FETCHER)
                        ),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ORDER_X, tb_1_.ORDER_Y, tb_1_.NAME " +
                                    "from ORDER_ tb_1_ " +
                                    "where (tb_1_.ORDER_X <> ? or tb_1_.ORDER_Y <> ?)"
                    );
                    ctx.statement(1).sql(
                            "select tb_1_.ORDER_ITEM_A, tb_1_.ORDER_ITEM_B, tb_1_.ORDER_ITEM_C, tb_1_.NAME " +
                                    "from ORDER_ITEM tb_1_ " +
                                    "where tb_1_.FK_ORDER_X = ? and tb_1_.FK_ORDER_Y = ?"
                    );
                    ctx.statement(2).sql(
                            "select " +
                                    "--->tb_2_.FK_ORDER_ITEM_A, tb_2_.FK_ORDER_ITEM_B, tb_2_.FK_ORDER_ITEM_C, " +
                                    "--->tb_1_.PRODUCT_ALPHA, tb_1_.PRODUCT_BETA, " +
                                    "--->tb_1_.NAME " +
                                    "from PRODUCT tb_1_ " +
                                    "inner join ORDER_ITEM_PRODUCT_MAPPING tb_2_ on " +
                                    "--->tb_1_.PRODUCT_ALPHA = tb_2_.FK_PRODUCT_ALPHA and " +
                                    "--->tb_1_.PRODUCT_BETA = tb_2_.FK_PRODUCT_BETA " +
                                    "where (tb_2_.FK_ORDER_ITEM_A, tb_2_.FK_ORDER_ITEM_B, tb_2_.FK_ORDER_ITEM_C) in (" +
                                    "--->(?, ?, ?), (?, ?, ?)" +
                                    ")"
                    );
                }
        );
    }
}
