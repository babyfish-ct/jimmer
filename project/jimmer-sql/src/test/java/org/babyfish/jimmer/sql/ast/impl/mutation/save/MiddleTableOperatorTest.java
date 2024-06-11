package org.babyfish.jimmer.sql.ast.impl.mutation.save;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.dialect.H2Dialect;
import org.babyfish.jimmer.sql.model.Objects;
import org.babyfish.jimmer.sql.model.embedded.OrderItemProps;
import org.babyfish.jimmer.sql.model.middle.CustomerProps;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.Arrays;

class MiddleTableOperatorTest extends AbstractMutationTest {

    @Test
    public void testDisconnectExcept() {
        connectAndExpect(
                con -> {
                    MiddleTableOperator operator = operator(getSqlClient(), con, CustomerProps.ORDINARY_SHOPS.unwrap());
                    return operator.disconnectExcept(
                            Arrays.asList(
                                    new Tuple2<>(1L, 3L),
                                    new Tuple2<>(2L, 4L)
                            )
                    );
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "delete from shop_customer_mapping " +
                                        "where (customer_id, shop_id) not in ((?, ?), (?, ?)) " +
                                        "and deleted_millis = ? " +
                                        "and type = ?"
                        );
                        it.variables(1L, 3L, 2L, 4L, 0L, "ORDINARY");
                    });
                    ctx.value("4");
                }
        );
    }

    @Test
    public void testDisconnectExceptByEmbedded() {
        connectAndExpect(
                con -> {
                    MiddleTableOperator operator = operator(getSqlClient(), con, OrderItemProps.PRODUCTS.unwrap());
                    return operator.disconnectExcept(
                            Arrays.asList(
                                    new Tuple2<>(
                                            Objects.createOrderItemId(id -> id.setA(1).setB(1).setC(1)),
                                            Objects.createProductId(id -> id.setAlpha("00A").setBeta("00A"))
                                    ),
                                    new Tuple2<>(
                                            Objects.createOrderItemId(id -> id.setA(1).setB(2).setC(1)),
                                            Objects.createProductId(id -> id.setAlpha("00A").setBeta("00B"))
                                    )
                            )
                    );
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "delete from ORDER_ITEM_PRODUCT_MAPPING " +
                                        "where (" +
                                        "--->FK_ORDER_ITEM_A, FK_ORDER_ITEM_B, FK_ORDER_ITEM_C, " +
                                        "--->FK_PRODUCT_ALPHA, FK_PRODUCT_BETA" +
                                        ") not in (" +
                                        "--->(?, ?, ?, ?, ?), " +
                                        "--->(?, ?, ?, ?, ?)" +
                                        ")"
                        );
                        it.variables(
                                1, 1, 1, "00A", "00A",
                                1, 2, 1, "00A", "00B"
                        );
                    });
                    ctx.value("6");
                }
        );
    }

    @Test
    public void testDisconnectExceptWithoutTupleSupport() {
        connectAndExpect(
                con -> {
                    MiddleTableOperator operator = operator(
                            getSqlClient(it -> it.setDialect(new H2Dialect() {
                                @Override
                                public boolean isTupleSupported() {
                                    return false;
                                }
                            })),
                            con,
                            OrderItemProps.PRODUCTS.unwrap()
                    );
                    return operator.disconnectExcept(
                            Arrays.asList(
                                    new Tuple2<>(
                                            Objects.createOrderItemId(id -> id.setA(1).setB(1).setC(1)),
                                            Objects.createProductId(id -> id.setAlpha("00A").setBeta("00A"))
                                    ),
                                    new Tuple2<>(
                                            Objects.createOrderItemId(id -> id.setA(1).setB(2).setC(1)),
                                            Objects.createProductId(id -> id.setAlpha("00A").setBeta("00B"))
                                    )
                            )
                    );
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "delete from ORDER_ITEM_PRODUCT_MAPPING " +
                                        "where (" +
                                        "--->FK_ORDER_ITEM_A <> ? or " +
                                        "--->FK_ORDER_ITEM_B <> ? or " +
                                        "--->FK_ORDER_ITEM_C <> ? or " +
                                        "--->FK_PRODUCT_ALPHA <> ? or " +
                                        "--->FK_PRODUCT_BETA <> ?" +
                                        ") and (" +
                                        "--->FK_ORDER_ITEM_A <> ? or " +
                                        "--->FK_ORDER_ITEM_B <> ? or " +
                                        "--->FK_ORDER_ITEM_C <> ? or " +
                                        "--->FK_PRODUCT_ALPHA <> ? or " +
                                        "--->FK_PRODUCT_BETA <> ?" +
                                        ")"
                        );
                        it.variables(
                                1, 1, 1, "00A", "00A",
                                1, 2, 1, "00A", "00B"
                        );
                    });
                    ctx.value("6");
                }
        );
    }

    private static MiddleTableOperator operator(
            JSqlClient sqlClient,
            Connection con,
            ImmutableProp prop
    ) {
        SaveOptionsImpl options = new SaveOptionsImpl((JSqlClientImplementor) sqlClient);
        return new MiddleTableOperator(
                new SaveContext(
                        options,
                        con,
                        prop.getDeclaringType()
                ).to(prop)
        );
    }
}
