package org.babyfish.jimmer.sql.ast.impl.mutation.save;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.mutation.AffectedTable;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.common.NativeDatabases;
import org.babyfish.jimmer.sql.dialect.H2Dialect;
import org.babyfish.jimmer.sql.dialect.MySqlDialect;
import org.babyfish.jimmer.sql.dialect.PostgresDialect;
import org.babyfish.jimmer.sql.model.BookProps;
import org.babyfish.jimmer.sql.model.Objects;
import org.babyfish.jimmer.sql.model.embedded.OrderItemProps;
import org.babyfish.jimmer.sql.model.middle.CustomerProps;
import org.babyfish.jimmer.sql.model.middle.ShopDraft;
import org.babyfish.jimmer.sql.model.middle.ShopProps;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.babyfish.jimmer.sql.runtime.ScalarProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.sql.*;
import java.util.*;

import static org.babyfish.jimmer.sql.common.Constants.*;

class MiddleTableOperatorTest extends AbstractMutationTest {

    // --------------------------
    // Non-Public methods tests
    // --------------------------

    @Test
    public void testFind() {
        connectAndExpect(
                con -> {
                    MiddleTableOperator operator = operator(getSqlClient(), con, ShopProps.ORDINARY_CUSTOMERS.unwrap());
                    return operator.find(Arrays.asList(1L, 2L));
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select shop_id, customer_id " +
                                        "from shop_customer_mapping " +
                                        "where shop_id = any(?) " +
                                        "and deleted_millis = ? " +
                                        "and type = ?"
                        );
                        it.variables(
                                new Object[] {1L, 2L},
                                0L,
                                "ORDINARY"
                        );
                        ctx.value(
                                "[" +
                                        "--->Tuple2(_1=1, _2=2), " +
                                        "--->Tuple2(_1=1, _2=3), " +
                                        "--->Tuple2(_1=2, _2=4), " +
                                        "--->Tuple2(_1=2, _2=5)" +
                                        "]"
                        );
                    });
                }
        );
    }

    @Test
    public void testFindWithoutAnyEqual() {
        connectAndExpect(
                con -> {
                    MiddleTableOperator operator = operator(
                            getSqlClient(it -> it.setDialect(
                                    new H2Dialect() {
                                        @Override
                                        public boolean isAnyEqualityOfArraySupported() {
                                            return false;
                                        }
                                    }
                            )),
                            con,
                            ShopProps.ORDINARY_CUSTOMERS.unwrap()
                    );
                    return operator.find(Arrays.asList(1L, 2L));
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select shop_id, customer_id " +
                                        "from shop_customer_mapping " +
                                        "where shop_id in (?, ?) " +
                                        "and deleted_millis = ? " +
                                        "and type = ?"
                        );
                        it.variables(1L, 2L, 0L, "ORDINARY");
                        ctx.value(
                                "[" +
                                        "--->Tuple2(_1=1, _2=2), " +
                                        "--->Tuple2(_1=1, _2=3), " +
                                        "--->Tuple2(_1=2, _2=4), " +
                                        "--->Tuple2(_1=2, _2=5)" +
                                        "]"
                        );
                    });
                }
        );
    }

    @Test
    public void testFindEmbeddable() {
        connectAndExpect(
                con -> {
                    MiddleTableOperator operator = operator(getSqlClient(), con, OrderItemProps.PRODUCTS.unwrap());
                    return operator.find(
                            Arrays.asList(
                                    Objects.createOrderItemId(id -> id.setA(1).setB(1).setC(1)),
                                    Objects.createOrderItemId(id -> id.setA(1).setB(2).setC(1))
                            )
                    );
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select " +
                                        "--->FK_ORDER_ITEM_A, FK_ORDER_ITEM_B, FK_ORDER_ITEM_C, " +
                                        "--->FK_PRODUCT_ALPHA, FK_PRODUCT_BETA " +
                                        "from ORDER_ITEM_PRODUCT_MAPPING " +
                                        "where (" +
                                        "--->FK_ORDER_ITEM_A, FK_ORDER_ITEM_B, FK_ORDER_ITEM_C" +
                                        ") in ((?, ?, ?), (?, ?, ?))"
                        );
                        it.variables(1, 1, 1, 1, 2, 1);
                    });
                    ctx.value(
                            "[" +
                                    "--->Tuple2(_1={\"a\":1,\"b\":1,\"c\":1}, _2={\"alpha\":\"00A\",\"beta\":\"00A\"}), " +
                                    "--->Tuple2(_1={\"a\":1,\"b\":1,\"c\":1}, _2={\"alpha\":\"00B\",\"beta\":\"00A\"}), " +
                                    "--->Tuple2(_1={\"a\":1,\"b\":2,\"c\":1}, _2={\"alpha\":\"00A\",\"beta\":\"00B\"}), " +
                                    "--->Tuple2(_1={\"a\":1,\"b\":2,\"c\":1}, _2={\"alpha\":\"00B\",\"beta\":\"00A\"})" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testDisconnect() {
        connectAndExpect(
                con -> {
                    MiddleTableOperator operator = operator(getSqlClient(), con, CustomerProps.ORDINARY_SHOPS.unwrap());
                    operator.disconnect(
                            IdPairs.of(
                                    new Tuple2<>(1L, 1L),
                                    new Tuple2<>(2L, 2L)
                            )
                    );
                    return operator.affectedRowCount;
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "delete from shop_customer_mapping " +
                                        "where customer_id = ? and shop_id = ?"
                        );
                        it.batchVariables(0, 1L, 1L);
                        it.batchVariables(1, 2L, 2L);
                    });
                    ctx.value(map -> {
                        Assertions.assertEquals(1, map.size());
                        Assertions.assertEquals(1, map.get(AffectedTable.of(ShopProps.ORDINARY_CUSTOMERS)));
                    });
                }
        );
    }

    @Test
    public void testDisconnectByEmbedded() {
        connectAndExpect(
                con -> {
                    MiddleTableOperator operator = operator(getSqlClient(), con, OrderItemProps.PRODUCTS.unwrap());
                    operator.disconnect(
                            IdPairs.of(
                                    new Tuple2<>(
                                            Objects.createOrderItemId(id -> id.setA(1).setB(1).setC(1)),
                                            Objects.createProductId(id -> id.setAlpha("00A").setBeta("00A"))
                                    ),
                                    new Tuple2<>(
                                            Objects.createOrderItemId(id -> id.setA(1).setB(2).setC(1)),
                                            Objects.createProductId(id -> id.setAlpha("00A").setBeta("00A"))
                                    )
                            )
                    );
                    return operator.affectedRowCount;
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "delete from ORDER_ITEM_PRODUCT_MAPPING " +
                                        "where " +
                                        "--->FK_ORDER_ITEM_A = ? " +
                                        "and " +
                                        "--->FK_ORDER_ITEM_B = ? " +
                                        "and " +
                                        "--->FK_ORDER_ITEM_C = ? " +
                                        "and " +
                                        "--->FK_PRODUCT_ALPHA = ? " +
                                        "and " +
                                        "--->FK_PRODUCT_BETA = ?"
                        );
                        it.batchVariables(0, 1, 1, 1, "00A", "00A");
                        it.batchVariables(1, 1, 2, 1, "00A", "00A");
                    });
                    ctx.value(map -> {
                        Assertions.assertEquals(1, map.size());
                        Assertions.assertEquals(1, map.get(AffectedTable.of(OrderItemProps.PRODUCTS)));
                    });
                }
        );
    }

    @Test
    public void testDisconnectExcept() {
        connectAndExpect(
                con -> {
                    MiddleTableOperator operator = operator(getSqlClient(), con, ShopProps.ORDINARY_CUSTOMERS.unwrap());
                    operator.disconnectExcept(
                            IdPairs.of(
                                    new Tuple2<>(1L, 3L),
                                    new Tuple2<>(2L, 4L)
                            )
                    );
                    return operator.affectedRowCount;
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "delete from shop_customer_mapping " +
                                        "where shop_id = ? and " +
                                        "not (customer_id = any(?)) " +
                                        "and deleted_millis = ? " +
                                        "and type = ?"
                        );
                        it.batchVariables(0, 1L, new Object[] { 3L }, 0L, "ORDINARY");
                        it.batchVariables(1, 2L, new Object[] { 4L }, 0L, "ORDINARY");
                    });
                    ctx.value(map -> {
                        Assertions.assertEquals(1, map.size());
                        Assertions.assertEquals(2, map.get(AffectedTable.of(ShopProps.ORDINARY_CUSTOMERS)));
                    });
                }
        );
    }

    @Test
    public void testDisconnectExceptOneSource() {
        connectAndExpect(
                con -> {
                    MiddleTableOperator operator = operator(getSqlClient(), con, ShopProps.ORDINARY_CUSTOMERS.unwrap());
                    operator.disconnectExcept(
                            IdPairs.of(
                                    new Tuple2<>(1L, 2L),
                                    new Tuple2<>(1L, 3L)
                            )
                    );
                    return operator.affectedRowCount;
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "delete from shop_customer_mapping " +
                                        "where shop_id = ? and " +
                                        "not (customer_id = any(?)) " +
                                        "and deleted_millis = ? " +
                                        "and type = ?"
                        );
                        it.variables(1L, new Object[] { 2L, 3L }, 0L, "ORDINARY");
                    });
                    ctx.value(map -> {
                        Assertions.assertTrue(map.isEmpty());
                    });
                }
        );
    }

    @Test
    public void testDisconnectExceptWithoutTargets() {
        connectAndExpect(
                con -> {
                    MiddleTableOperator operator = operator(getSqlClient(), con, ShopProps.ORDINARY_CUSTOMERS.unwrap());
                    operator.disconnectExcept(
                            IdPairs.of(
                                    Arrays.asList(
                                            (ImmutableSpi) ShopDraft.$.produce(draft -> {
                                                draft.setId(1L);
                                                draft.setOrdinaryCustomers(Collections.emptyList());
                                            }),
                                            (ImmutableSpi) ShopDraft.$.produce(draft -> {
                                                draft.setId(2L);
                                                draft.setOrdinaryCustomers(Collections.emptyList());
                                            })
                                    ),
                                    ShopProps.ORDINARY_CUSTOMERS.unwrap()
                            )
                    );
                    return operator.affectedRowCount;
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "delete from shop_customer_mapping " +
                                        "where shop_id = ? " +
                                        "and not (customer_id = any(?)) " +
                                        "and deleted_millis = ? " +
                                        "and type = ?"
                        );
                        it.batchVariables(0, 1L, new Object[0], 0L, "ORDINARY");
                        it.batchVariables(1, 2L, new Object[0], 0L, "ORDINARY");
                    });
                    ctx.value(map -> {
                        Assertions.assertEquals(1, map.size());
                        Assertions.assertEquals(4, map.get(AffectedTable.of(ShopProps.ORDINARY_CUSTOMERS)));
                    });
                }
        );
    }

    @Test
    public void testDisconnectExceptByEmbedded() {
        connectAndExpect(
                con -> {
                    MiddleTableOperator operator = operator(getSqlClient(), con, OrderItemProps.PRODUCTS.unwrap());
                    operator.disconnectExcept(
                            IdPairs.of(
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
                    return operator.affectedRowCount;
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "delete from ORDER_ITEM_PRODUCT_MAPPING " +
                                        "where " +
                                        "--->(FK_ORDER_ITEM_A, FK_ORDER_ITEM_B, FK_ORDER_ITEM_C) " +
                                        "--->in ((?, ?, ?), (?, ?, ?)) " +
                                        "and " +
                                        "--->(FK_ORDER_ITEM_A, FK_ORDER_ITEM_B, FK_ORDER_ITEM_C, " +
                                        "--->FK_PRODUCT_ALPHA, FK_PRODUCT_BETA) " +
                                        "--->not in ((?, ?, ?, ?, ?), (?, ?, ?, ?, ?))"
                        );
                        it.variables(
                                1, 1, 1,
                                1, 2, 1,
                                1, 1, 1, "00A", "00A",
                                1, 2, 1, "00A", "00B"
                        );
                    });
                    ctx.value(map -> {
                        Assertions.assertEquals(1, map.size());
                        Assertions.assertEquals(2, map.get(AffectedTable.of(OrderItemProps.PRODUCTS)));
                    });
                }
        );
    }

    @Test
    public void testDisconnectExceptByEmbeddedWithoutTargets() {
        connectAndExpect(
                con -> {
                    MiddleTableOperator operator = operator(getSqlClient(), con, OrderItemProps.PRODUCTS.unwrap());
                    operator.disconnectExcept(
                            IdPairs.of(
                                    Arrays.asList(
                                            (ImmutableSpi) Objects.createOrderItem(draft -> {
                                                draft.applyId(id -> id.setA(1).setB(1).setC(1));
                                                draft.setProducts(Collections.emptyList());
                                            }),
                                            (ImmutableSpi) Objects.createOrderItem(draft -> {
                                                draft.applyId(id -> id.setA(1).setB(2).setC(1));
                                                draft.setProducts(Collections.emptyList());
                                            })
                                    ),
                                    OrderItemProps.PRODUCTS.unwrap()
                            )
                    );
                    return operator.affectedRowCount;
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "delete from ORDER_ITEM_PRODUCT_MAPPING " +
                                        "where (FK_ORDER_ITEM_A, FK_ORDER_ITEM_B, FK_ORDER_ITEM_C) " +
                                        "in ((?, ?, ?), (?, ?, ?))"
                        );
                        it.variables(
                                1, 1, 1,
                                1, 2, 1
                        );
                    });
                    ctx.value(map -> {
                        Assertions.assertEquals(1, map.size());
                        Assertions.assertEquals(4, map.get(AffectedTable.of(OrderItemProps.PRODUCTS)));
                    });
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
                    operator.disconnectExcept(
                            IdPairs.of(
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
                    return operator.affectedRowCount;
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "delete from ORDER_ITEM_PRODUCT_MAPPING " +
                                        "where (" +
                                        "--->--->FK_ORDER_ITEM_A = ? and FK_ORDER_ITEM_B = ? and FK_ORDER_ITEM_C = ? " +
                                        "--->or " +
                                        "--->--->FK_ORDER_ITEM_A = ? and FK_ORDER_ITEM_B = ? and FK_ORDER_ITEM_C = ?" +
                                        ") and (" +
                                        "--->FK_ORDER_ITEM_A <> ? or FK_ORDER_ITEM_B <> ? or FK_ORDER_ITEM_C <> ? or " +
                                        "--->FK_PRODUCT_ALPHA <> ? or FK_PRODUCT_BETA <> ?" +
                                        ") and (" +
                                        "--->FK_ORDER_ITEM_A <> ? or FK_ORDER_ITEM_B <> ? or FK_ORDER_ITEM_C <> ? or " +
                                        "--->FK_PRODUCT_ALPHA <> ? or FK_PRODUCT_BETA <> ?" +
                                        ")"
                        );
                        it.variables(
                                1, 1, 1,
                                1, 2, 1,
                                1, 1, 1, "00A", "00A",
                                1, 2, 1, "00A", "00B"
                        );
                    });
                    ctx.value(map -> {
                        Assertions.assertEquals(1, map.size());
                        Assertions.assertEquals(2, map.get(AffectedTable.of(OrderItemProps.PRODUCTS)));
                    });
                }
        );
    }

    @Test
    public void testConnect() {
        connectAndExpect(
                con -> {
                    MiddleTableOperator operator = operator(getSqlClient(), con, CustomerProps.VIP_SHOPS.unwrap());
                    operator.connect(
                            IdPairs.of(
                                    new Tuple2<>(1L, 2L),
                                    new Tuple2<>(2L, 2L)
                            )
                    );
                    return operator.affectedRowCount;
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "insert into shop_customer_mapping(" +
                                        "--->customer_id, shop_id, deleted_millis, type" +
                                        ") values(?, ?, ?, ?)"
                        );
                        it.batchVariables(0, 1L, 2L, 0L, "VIP");
                        it.batchVariables(1, 2L, 2L, 0L, "VIP");
                    });
                    ctx.value(map -> {
                        Assertions.assertEquals(1, map.size());
                        Assertions.assertEquals(2, map.get(AffectedTable.of(ShopProps.VIP_CUSTOMERS)));
                    });
                }
        );
    }

    @Test
    public void testConnectByEmbedded() {
        connectAndExpect(
                con -> {
                    MiddleTableOperator operator = operator(getSqlClient(), con, OrderItemProps.PRODUCTS.unwrap());
                    operator.connect(
                            IdPairs.of(
                                    new Tuple2<>(
                                            Objects.createOrderItemId(id -> id.setA(9).setB(9).setC(9)),
                                            Objects.createProductId(id -> id.setAlpha("00A").setBeta("00A"))
                                    ),
                                    new Tuple2<>(
                                            Objects.createOrderItemId(id -> id.setA(9).setB(9).setC(9)),
                                            Objects.createProductId(id -> id.setAlpha("00A").setBeta("00B"))
                                    )
                            )
                    );
                    return operator.affectedRowCount;
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "insert into ORDER_ITEM_PRODUCT_MAPPING(" +
                                        "--->FK_ORDER_ITEM_A, FK_ORDER_ITEM_B, FK_ORDER_ITEM_C, " +
                                        "--->FK_PRODUCT_ALPHA, FK_PRODUCT_BETA" +
                                        ") values(?, ?, ?, ?, ?)"
                        );
                        it.batchVariables(0, 9, 9, 9, "00A", "00A");
                        it.batchVariables(1, 9, 9, 9, "00A", "00B");
                    });
                    ctx.value(map -> {
                        Assertions.assertEquals(1, map.size());
                        Assertions.assertEquals(2, map.get(AffectedTable.of(OrderItemProps.PRODUCTS)));
                    });
                }
        );
    }

    @Test
    public void testConnectIfNecessary() {
        connectAndExpect(
                con -> {
                    MiddleTableOperator operator = operator(getSqlClient(), con, CustomerProps.VIP_SHOPS.unwrap());
                    int[] rowCounts = operator.connectIfNecessary(
                            IdPairs.of(
                                    new Tuple2<>(1L, 2L),
                                    new Tuple2<>(2L, 2L)
                            )
                    );
                    int sumRowCount = 0;
                    for (int rowCount : rowCounts) {
                        sumRowCount += rowCount;
                    }
                    return sumRowCount;
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into shop_customer_mapping(" +
                                        "--->customer_id, shop_id, deleted_millis, type" +
                                        ") key(" +
                                        "--->customer_id, shop_id, deleted_millis, type" +
                                        ") values(?, ?, ?, ?)"
                        );
                        it.batchVariables(0, 1L, 2L, 0L, "VIP");
                        it.batchVariables(1, 2L, 2L, 0L, "VIP");
                    });
                    ctx.value("2");
                }
        );
    }

    @Test
    public void testConnectIfNecessaryByEmbedded() {
        connectAndExpect(
                con -> {
                    MiddleTableOperator operator = operator(getSqlClient(), con, OrderItemProps.PRODUCTS.unwrap());
                    int[] rowCounts = operator.connectIfNecessary(
                            IdPairs.of(
                                    new Tuple2<>(
                                            Objects.createOrderItemId(id -> id.setA(1).setB(1).setC(1)),
                                            Objects.createProductId(id -> id.setAlpha("00A").setBeta("00A"))
                                    ),
                                    new Tuple2<>(
                                            Objects.createOrderItemId(id -> id.setA(9).setB(9).setC(9)),
                                            Objects.createProductId(id -> id.setAlpha("00A").setBeta("00B"))
                                    )
                            )
                    );
                    int sumRowCount = 0;
                    for (int rowCount : rowCounts) {
                        sumRowCount += rowCount;
                    }
                    return sumRowCount;
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into ORDER_ITEM_PRODUCT_MAPPING(" +
                                        "--->FK_ORDER_ITEM_A, FK_ORDER_ITEM_B, FK_ORDER_ITEM_C, " +
                                        "--->FK_PRODUCT_ALPHA, FK_PRODUCT_BETA" +
                                        ") key(" +
                                        "--->FK_ORDER_ITEM_A, FK_ORDER_ITEM_B, FK_ORDER_ITEM_C, " +
                                        "--->FK_PRODUCT_ALPHA, FK_PRODUCT_BETA" +
                                        ") values(?, ?, ?, ?, ?)"
                        );
                        it.batchVariables(0, 1, 1, 1, "00A", "00A");
                        it.batchVariables(1, 9, 9, 9, "00A", "00B");
                    });
                    ctx.value("2");
                }
        );
    }

    // --------------------------
    // Public methods tests
    // --------------------------

    @Test
    public void testAppend() {
        connectAndExpect(
                con -> {
                    MiddleTableOperator operator = operator(getSqlClient(), con, BookProps.AUTHORS.unwrap());
                    operator.append(
                            IdPairs.of(
                                    new Tuple2<>(learningGraphQLId1, borisId),
                                    new Tuple2<>(learningGraphQLId1, danId)
                            )
                    );
                    return operator.affectedRowCount;
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "insert into BOOK_AUTHOR_MAPPING(BOOK_ID, AUTHOR_ID) " +
                                        "values(?, ?)"
                        );
                        it.batchVariables(0, learningGraphQLId1, borisId);
                        it.batchVariables(1, learningGraphQLId1, danId);
                    });
                    ctx.value(map -> {
                        Assertions.assertEquals(1, map.size());
                        Assertions.assertEquals(2, map.get(AffectedTable.of(BookProps.AUTHORS)));
                    });
                }
        );
    }

    @Test
    public void testMerge() {
        connectAndExpect(
                con -> {
                    MiddleTableOperator operator = operator(
                            getSqlClient(),
                            con,
                            BookProps.AUTHORS.unwrap()
                    );
                    operator.merge(
                            IdPairs.of(
                                    new Tuple2<>(learningGraphQLId1, alexId),
                                    new Tuple2<>(learningGraphQLId1, borisId),
                                    new Tuple2<>(learningGraphQLId2, alexId),
                                    new Tuple2<>(learningGraphQLId2, borisId)
                            )
                    );
                    assertAuthorIds(con, true, learningGraphQLId1, new UUID[] { eveId, alexId, borisId });
                    return operator.affectedRowCount;
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select BOOK_ID, AUTHOR_ID " +
                                        "from BOOK_AUTHOR_MAPPING " +
                                        "where BOOK_ID = any(?)"
                        );
                        it.variables((Object) new Object[]{ learningGraphQLId1, learningGraphQLId2 });
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into BOOK_AUTHOR_MAPPING(BOOK_ID, AUTHOR_ID) " +
                                        "values(?, ?)"
                        );
                        it.batchVariables(0, learningGraphQLId1, borisId);
                        it.batchVariables(1, learningGraphQLId2, borisId);
                    });
                    ctx.value(map -> {
                        Assertions.assertEquals(1, map.size());
                        Assertions.assertEquals(2, map.get(AffectedTable.of(BookProps.AUTHORS)));
                    });
                }
        );
    }

    @Test
    public void testMergeByMySql() {

        NativeDatabases.assumeNativeDatabase();

        connectAndExpect(
                NativeDatabases.MYSQL_DATA_SOURCE,
                con -> {
                    MiddleTableOperator operator = operator(
                            getSqlClient(it -> {
                                it.setDialect(new MySqlDialect());
                                it.addScalarProvider(ScalarProvider.uuidByByteArray());
                            }),
                            con,
                            BookProps.AUTHORS.unwrap()
                    );
                    operator.merge(
                            IdPairs.of(
                                    new Tuple2<>(learningGraphQLId1, alexId),
                                    new Tuple2<>(learningGraphQLId1, borisId)
                            )
                    );
                    assertAuthorIds(con, true, learningGraphQLId1, new UUID[] { eveId, alexId, borisId });
                    return operator.affectedRowCount;
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "insert ignore into BOOK_AUTHOR_MAPPING(" +
                                        "--->BOOK_ID, AUTHOR_ID" +
                                        ") values(?, ?)"
                        );
                        it.batchVariables(0, toByteArray(learningGraphQLId1), toByteArray(alexId));
                        it.batchVariables(1, toByteArray(learningGraphQLId1), toByteArray(borisId));
                    });
                    ctx.value(map -> {
                        Assertions.assertEquals(1, map.size());
                        Assertions.assertEquals(1, map.get(AffectedTable.of(BookProps.AUTHORS)));
                    });
                }
        );
    }

    @Test
    public void testMergeByPostgres() {

        NativeDatabases.assumeNativeDatabase();

        connectAndExpect(
                NativeDatabases.POSTGRES_DATA_SOURCE,
                con -> {
                    MiddleTableOperator operator = operator(
                            getSqlClient(it -> {
                                it.setDialect(new PostgresDialect());
                            }),
                            con,
                            BookProps.AUTHORS.unwrap()
                    );
                    operator.merge(
                            IdPairs.of(
                                    new Tuple2<>(learningGraphQLId1, alexId),
                                    new Tuple2<>(learningGraphQLId1, borisId)
                            )
                    );
                    assertAuthorIds(con, false, learningGraphQLId1, new UUID[] { eveId, alexId, borisId });
                    return operator.affectedRowCount;
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "insert into BOOK_AUTHOR_MAPPING(BOOK_ID, AUTHOR_ID) " +
                                        "values(?, ?) on conflict(BOOK_ID, AUTHOR_ID) do nothing"
                        );
                        it.batchVariables(0, learningGraphQLId1, alexId);
                        it.batchVariables(1, learningGraphQLId1, borisId);
                    });
                    ctx.value(map -> {
                        Assertions.assertEquals(1, map.size());
                        Assertions.assertEquals(1, map.get(AffectedTable.of(BookProps.AUTHORS)));
                    });
                }
        );
    }

    @Test
    public void testMergeComplexTable() {
        connectAndExpect(
                con -> {
                    MiddleTableOperator operator = operator(
                            getSqlClient(),
                            con,
                            ShopProps.VIP_CUSTOMERS.unwrap()
                    );
                    operator.merge(
                            IdPairs.of(
                                    new Tuple2<>(1L, 1L), // Existing
                                    new Tuple2<>(1L, 2L),
                                    new Tuple2<>(2L, 2L), // Existing
                                    new Tuple2<>(2L, 3L)
                            )
                    );
                    assertVipCustomerIds(con, 1L, new long[] {1L, 2L});
                    assertVipCustomerIds(con, 2L, new long[] {2L, 3L});
                    return operator.affectedRowCount;
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select shop_id, customer_id " +
                                        "from shop_customer_mapping " +
                                        "where shop_id = any(?) " +
                                        "and deleted_millis = ? " +
                                        "and type = ?"
                        );
                        it.variables(new Object[]{1L, 2L}, 0L, "VIP");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into shop_customer_mapping(" +
                                        "--->shop_id, customer_id, deleted_millis, type" +
                                        ") values(?, ?, ?, ?)"
                        );
                        it.batchVariables(0, 1L, 2L, 0L, "VIP");
                        it.batchVariables(1, 2L, 2L, 0L, "VIP");
                    });
                    ctx.value(map -> {
                        Assertions.assertEquals(1, map.size());
                        Assertions.assertEquals(2, map.get(AffectedTable.of(ShopProps.VIP_CUSTOMERS)));
                    });
                }
        );
    }

    @Test
    public void testReplace() {
        connectAndExpect(
                con -> {
                    MiddleTableOperator operator = operator(
                            getSqlClient(),
                            con,
                            BookProps.AUTHORS.unwrap()
                    );
                    operator.replace(
                            IdPairs.of(
                                    new Tuple2<>(learningGraphQLId2, alexId),
                                    new Tuple2<>(learningGraphQLId2, danId),
                                    new Tuple2<>(learningGraphQLId1, alexId),
                                    new Tuple2<>(learningGraphQLId1, danId)
                            )
                    );
                    assertAuthorIds(
                            con,
                            false,
                            learningGraphQLId1,
                            new UUID[] { alexId, danId }
                    );
                    assertAuthorIds(
                            con,
                            false,
                            learningGraphQLId2,
                            new UUID[] { alexId, danId }
                    );
                    return operator.affectedRowCount;
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select BOOK_ID, AUTHOR_ID " +
                                        "from BOOK_AUTHOR_MAPPING " +
                                        "where BOOK_ID = any(?)"
                        );
                        it.variables((Object) new Object[] {learningGraphQLId2, learningGraphQLId1});
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from BOOK_AUTHOR_MAPPING where BOOK_ID = ? and AUTHOR_ID = ?"
                        );
                        it.batchVariables(0, learningGraphQLId2, eveId);
                        it.batchVariables(1, learningGraphQLId1, eveId);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into BOOK_AUTHOR_MAPPING(BOOK_ID, AUTHOR_ID) " +
                                        "values(?, ?)"
                        );
                        it.batchVariables(0, learningGraphQLId2, danId);
                        it.batchVariables(1, learningGraphQLId1, danId);
                    });
                    ctx.value(map -> {
                        Assertions.assertEquals(1, map.size());
                        Assertions.assertEquals(4, map.get(AffectedTable.of(BookProps.AUTHORS)));
                    });
                }
        );
    }

    @Test
    public void testReplaceByMySql() {

        NativeDatabases.assumeNativeDatabase();

        connectAndExpect(
                NativeDatabases.MYSQL_DATA_SOURCE,
                con -> {
                    MiddleTableOperator operator = operator(
                            getSqlClient(it -> {
                                it.setDialect(new MySqlDialect());
                                it.addScalarProvider(ScalarProvider.uuidByByteArray());
                            }),
                            con,
                            BookProps.AUTHORS.unwrap()
                    );
                    operator.replace(
                            IdPairs.of(
                                    new Tuple2<>(learningGraphQLId2, alexId),
                                    new Tuple2<>(learningGraphQLId2, danId)
                            )
                    );
                    assertAuthorIds(
                            con,
                            true,
                            learningGraphQLId2,
                            new UUID[] { alexId, danId }
                    );
                    return operator.affectedRowCount;
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "delete from BOOK_AUTHOR_MAPPING " +
                                        "where BOOK_ID = ? " +
                                        "and AUTHOR_ID not in (?, ?)"
                        );
                        it.variables(
                                toByteArray(learningGraphQLId2),
                                toByteArray(alexId),
                                toByteArray(danId)
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert ignore into BOOK_AUTHOR_MAPPING(BOOK_ID, AUTHOR_ID) " +
                                        "values(?, ?)"
                        );
                        it.batchVariables(0, toByteArray(learningGraphQLId2), toByteArray(alexId));
                        it.batchVariables(1, toByteArray(learningGraphQLId2), toByteArray(danId));
                    });
                    ctx.value(map -> {
                        Assertions.assertEquals(1, map.size());
                        Assertions.assertEquals(2, map.get(AffectedTable.of(BookProps.AUTHORS)));
                    });
                }
        );
    }

    private static void assertAuthorIds(Connection con, boolean uuidToBytes, UUID bookId, UUID[] authorIds) {
        String sql = "select author_id from book_author_mapping where book_id = ?";
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            if (uuidToBytes) {
                stmt.setBytes(1, ScalarProvider.uuidByByteArray().toSql(bookId));
            } else {
                stmt.setObject(1, bookId);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                Set<UUID> set = new HashSet<>();
                while (rs.next()) {
                    if (uuidToBytes) {
                        set.add(ScalarProvider.uuidByByteArray().toScalar(rs.getBytes(1)));
                    } else {
                        set.add(rs.getObject(1, UUID.class));
                    }
                }
                Assertions.assertEquals(authorIds.length, set.size(), "Illegal author id count");
                for (UUID authorId : authorIds) {
                    Assertions.assertTrue(set.contains(authorId));
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static void assertVipCustomerIds(Connection con, long shopId, long[] vipCustomerIds) {
        String sql =
                "select customer_id from shop_customer_mapping " +
                        "where shop_id = ? " +
                        "and deleted_millis = 0 " +
                        "and type = ?";
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setLong(1, shopId);
            stmt.setObject(2, "VIP");
            try (ResultSet rs = stmt.executeQuery()) {
                Set<Long> set = new HashSet<>();
                while (rs.next()) {
                    set.add(rs.getLong(1));
                }
                Assertions.assertEquals(vipCustomerIds.length, set.size(), "Illegal author id count");
                for (long vipCustomerId : vipCustomerIds) {
                    Assertions.assertTrue(set.contains(vipCustomerId));
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static byte[] toByteArray(UUID uuid) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[16]);
        byteBuffer.putLong(uuid.getMostSignificantBits());
        byteBuffer.putLong(uuid.getLeastSignificantBits());
        return byteBuffer.array();
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
                ).prop(prop)
        );
    }

    @Override
    protected boolean isAnyEqualityOfArraySupported() {
        return true;
    }
}
