package org.babyfish.jimmer.sql.ast.impl.mutation.save;

import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.ast.mutation.AffectedTable;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.dialect.H2Dialect;
import org.babyfish.jimmer.sql.model.*;
import org.babyfish.jimmer.sql.model.Objects;
import org.babyfish.jimmer.sql.model.embedded.OrderItem;
import org.babyfish.jimmer.sql.model.embedded.OrderItemProps;
import org.babyfish.jimmer.sql.model.flat.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.babyfish.jimmer.sql.common.Constants.*;

public class DeleteChildTest extends AbstractChildOperatorTest {

    @Test
    public void testDisconnectExceptBySimpleInPredicate() {
        connectAndExpect(
                con -> {
                    ChildTableOperator operator = operator(
                            getSqlClient(it -> it.setMaxCommandJoinCount(3)),
                            con,
                            BookProps.STORE.unwrap(),
                            DissociateAction.DELETE
                    );
                    operator.disconnectExcept(
                            IdPairs.of(
                                    Arrays.asList(
                                            new Tuple2<>(manningId, graphQLInActionId1),
                                            new Tuple2<>(manningId, graphQLInActionId2)
                                    )
                            )
                    );
                    return operator.ctx.affectedRowCountMap;
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "delete from BOOK_AUTHOR_MAPPING tb_1_ " +
                                        "where exists (" +
                                        "--->select * " +
                                        "--->from BOOK tb_2_ " +
                                        "--->where " +
                                        "--->--->tb_1_.BOOK_ID = tb_2_.ID " +
                                        "--->and " +
                                        "--->--->tb_2_.STORE_ID = ? " +
                                        "--->and " +
                                        "--->--->tb_2_.ID not in (?, ?)" +
                                        ")"
                        );
                        it.variables(manningId, graphQLInActionId1, graphQLInActionId2);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from BOOK " +
                                        "where STORE_ID = ? and ID not in (?, ?)"
                        );
                        it.variables(manningId, graphQLInActionId1, graphQLInActionId2);
                    });
                    ctx.value(map -> {
                        Assertions.assertEquals(2, map.size());
                        Assertions.assertEquals(1, map.get(AffectedTable.of(BookProps.AUTHORS)));
                        Assertions.assertEquals(1, map.get(AffectedTable.of(Book.class)));
                    });
                }
        );
    }

    @Test
    public void testDisconnectExceptByComplexInPredicate() {
        connectAndExpect(
                con -> {
                    ChildTableOperator operator = operator(
                            getSqlClient(),
                            con,
                            BookProps.STORE.unwrap(),
                            DissociateAction.DELETE
                    );
                    operator.disconnectExcept(
                            IdPairs.of(
                                    Arrays.asList(
                                            new Tuple2<>(oreillyId, learningGraphQLId1),
                                            new Tuple2<>(oreillyId, effectiveTypeScriptId1),
                                            new Tuple2<>(oreillyId, programmingTypeScriptId1),
                                            new Tuple2<>(manningId, graphQLInActionId1)
                                    )
                            )
                    );
                    return operator.ctx.affectedRowCountMap;
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "delete from BOOK_AUTHOR_MAPPING tb_1_ " +
                                        "where exists (" +
                                        "--->select * " +
                                        "--->from BOOK tb_2_ " +
                                        "--->where " +
                                        "--->--->tb_1_.BOOK_ID = tb_2_.ID " +
                                        "--->and " +
                                        "--->--->tb_2_.STORE_ID in (?, ?) " +
                                        "--->and " +
                                        "--->--->(tb_2_.STORE_ID, tb_2_.ID) not in ((?, ?), (?, ?), (?, ?), (?, ?))" +
                                        ")"
                        );
                        it.variables(
                                oreillyId, manningId,
                                oreillyId, learningGraphQLId1,
                                oreillyId, effectiveTypeScriptId1,
                                oreillyId, programmingTypeScriptId1,
                                manningId, graphQLInActionId1
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from BOOK where " +
                                        "--->STORE_ID in (?, ?) " +
                                        "and " +
                                        "--->(STORE_ID, ID) not in ((?, ?), (?, ?), (?, ?), (?, ?))"
                        );
                        it.variables(
                                oreillyId, manningId,
                                oreillyId, learningGraphQLId1,
                                oreillyId, effectiveTypeScriptId1,
                                oreillyId, programmingTypeScriptId1,
                                manningId, graphQLInActionId1
                        );
                    });
                    ctx.value(map -> {
                        Assertions.assertEquals(2, map.size());
                        Assertions.assertEquals(10, map.get(AffectedTable.of(BookProps.AUTHORS)));
                        Assertions.assertEquals(8, map.get(AffectedTable.of(Book.class)));
                    });
                }
        );
    }

    @Test
    public void testDisconnectExceptByBatch() {
        connectAndExpect(
                con -> {
                    ChildTableOperator operator = operator(
                            getSqlClient(it -> it.setDialect(new H2Dialect())),
                            con,
                            BookProps.STORE.unwrap(),
                            DissociateAction.DELETE
                    );
                    operator.disconnectExcept(
                            IdPairs.of(
                                    Arrays.asList(
                                            new Tuple2<>(oreillyId, learningGraphQLId1),
                                            new Tuple2<>(oreillyId, effectiveTypeScriptId1),
                                            new Tuple2<>(oreillyId, programmingTypeScriptId1),
                                            new Tuple2<>(manningId, graphQLInActionId1)
                                    )
                            )
                    );
                    return operator.ctx.affectedRowCountMap;
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "delete from BOOK_AUTHOR_MAPPING tb_1_ " +
                                        "where exists (" +
                                        "--->select * " +
                                        "--->from BOOK tb_2_ " +
                                        "--->where " +
                                        "--->--->tb_1_.BOOK_ID = tb_2_.ID " +
                                        "--->and " +
                                        "--->--->STORE_ID = ? " +
                                        "--->and " +
                                        "--->--->not (ID = any(?))" +
                                        ")"
                        );
                        it.batchVariables(
                                0, oreillyId, new Object[] {
                                        learningGraphQLId1,
                                        effectiveTypeScriptId1,
                                        programmingTypeScriptId1
                                }
                        );
                        it.batchVariables(
                                1, manningId, new Object[] {
                                        graphQLInActionId1
                                }
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from BOOK " +
                                        "where STORE_ID = ? and not (ID = any(?))"
                        );
                        it.batchVariables(
                                0, oreillyId, new Object[] {
                                        learningGraphQLId1,
                                        effectiveTypeScriptId1,
                                        programmingTypeScriptId1
                                }
                        );
                        it.batchVariables(
                                1, manningId, new Object[] {
                                        graphQLInActionId1
                                }
                        );
                    });
                    ctx.value(map -> {
                        Assertions.assertEquals(2, map.size());
                        Assertions.assertEquals(10, map.get(AffectedTable.of(BookProps.AUTHORS)));
                        Assertions.assertEquals(8, map.get(AffectedTable.of(Book.class)));
                    });
                }
        );
    }

    @Test
    public void testDisconnectExceptBySelect() {
        connectAndExpect(
                con -> {
                    ChildTableOperator operator = operator(
                            getSqlClient(it -> {
                                it.setMaxCommandJoinCount(1);
                                it.setDialect(new H2Dialect());
                            }),
                            con,
                            BookProps.STORE.unwrap(),
                            DissociateAction.DELETE
                    );
                    operator.disconnectExcept(
                            IdPairs.of(
                                    Arrays.asList(
                                            new Tuple2<>(oreillyId, learningGraphQLId1),
                                            new Tuple2<>(oreillyId, effectiveTypeScriptId1),
                                            new Tuple2<>(oreillyId, programmingTypeScriptId1),
                                            new Tuple2<>(manningId, graphQLInActionId1)
                                    )
                            )
                    );
                    return operator.ctx.affectedRowCountMap;
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select BOOK_ID, AUTHOR_ID " +
                                        "from BOOK_AUTHOR_MAPPING tb_1_ " +
                                        "inner join BOOK tb_2_ on tb_1_.BOOK_ID = tb_2_.ID " +
                                        "where " +
                                        "--->tb_2_.STORE_ID = any(?) " +
                                        "and " +
                                        "--->(tb_2_.STORE_ID, tb_2_.ID) not in ((?, ?), (?, ?), (?, ?), (?, ?))"
                        );
                        it.variables(
                                new Object[] { oreillyId, manningId },
                                oreillyId, learningGraphQLId1,
                                oreillyId, effectiveTypeScriptId1,
                                oreillyId, programmingTypeScriptId1,
                                manningId, graphQLInActionId1
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from BOOK_AUTHOR_MAPPING " +
                                        "where BOOK_ID = ? and AUTHOR_ID = ?"
                        );
                        it.batchVariables(0, graphQLInActionId2, sammerId);
                        it.batchVariables(1, graphQLInActionId3, sammerId);
                        it.batchVariables(2, learningGraphQLId2, alexId);
                        it.batchVariables(3, learningGraphQLId2, eveId);
                        it.batchVariables(4, learningGraphQLId3, alexId);
                        it.batchVariables(5, learningGraphQLId3, eveId);
                        it.batchVariables(6, effectiveTypeScriptId2, danId);
                        it.batchVariables(7, effectiveTypeScriptId3, danId);
                        it.batchVariables(8, programmingTypeScriptId2, borisId);
                        it.batchVariables(9, programmingTypeScriptId3, borisId);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from BOOK " +
                                        "where STORE_ID = ? and not (ID = any(?))"
                        );
                        it.batchVariables(
                                0, oreillyId, new Object[] {
                                        learningGraphQLId1,
                                        effectiveTypeScriptId1,
                                        programmingTypeScriptId1
                                }
                        );
                        it.batchVariables(
                                1, manningId, new Object[] {
                                        graphQLInActionId1
                                }
                        );
                    });
                    ctx.value(map -> {
                        Assertions.assertEquals(2, map.size());
                        Assertions.assertEquals(10, map.get(AffectedTable.of(BookProps.AUTHORS)));
                        Assertions.assertEquals(8, map.get(AffectedTable.of(Book.class)));
                    });
                }
        );
    }

    @Test
    public void testDisconnectExceptBySimpleInPredicateAndEmbedded() {
        connectAndExpect(
                con -> {
                    ChildTableOperator operator = operator(
                            getSqlClient(),
                            con,
                            OrderItemProps.ORDER.unwrap(),
                            DissociateAction.DELETE
                    );
                    operator.disconnectExcept(
                            IdPairs.of(
                                    Arrays.asList(
                                            new Tuple2<>(
                                                    Objects.createOrderId(draft -> draft.setX("001").setY("001")),
                                                    Objects.createOrderItemId(draft -> draft.setA(1).setB(1).setC(1))
                                            )
                                    )
                            )
                    );
                    return operator.ctx.affectedRowCountMap;
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "delete from ORDER_ITEM_PRODUCT_MAPPING tb_1_ " +
                                        "where exists (" +
                                        "--->select * " +
                                        "--->from ORDER_ITEM tb_2_ " +
                                        "--->where " +
                                        "--->--->tb_1_.FK_ORDER_ITEM_A = tb_2_.ORDER_ITEM_A " +
                                        "--->and " +
                                        "--->--->tb_1_.FK_ORDER_ITEM_B = tb_2_.ORDER_ITEM_B " +
                                        "--->and " +
                                        "--->--->tb_1_.FK_ORDER_ITEM_C = tb_2_.ORDER_ITEM_C " +
                                        "--->and " +
                                        "--->--->(tb_2_.FK_ORDER_X, tb_2_.FK_ORDER_Y) = (?, ?) " +
                                        "--->and " +
                                        "--->--->(tb_2_.ORDER_ITEM_A, tb_2_.ORDER_ITEM_B, tb_2_.ORDER_ITEM_C) <> (?, ?, ?)" +
                                        ")"
                        );
                        it.variables(
                                "001", "001", 1, 1, 1
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from ORDER_ITEM " +
                                        "where " +
                                        "--->(FK_ORDER_X, FK_ORDER_Y) = (?, ?) " +
                                        "and " +
                                        "--->(ORDER_ITEM_A, ORDER_ITEM_B, ORDER_ITEM_C) <> (?, ?, ?)"
                        );
                        it.variables(
                                "001", "001", 1, 1, 1
                        );
                    });
                    ctx.value(map -> {
                        Assertions.assertEquals(2, map.size());
                        Assertions.assertEquals(2, map.get(AffectedTable.of(OrderItemProps.PRODUCTS)));
                        Assertions.assertEquals(1, map.get(AffectedTable.of(OrderItem.class)));
                    });
                }
        );
    }

    @Test
    public void testDisconnectExceptByComplexInPredicateAndEmbedded() {
        connectAndExpect(
                con -> {
                    ChildTableOperator operator = operator(
                            getSqlClient(),
                            con,
                            OrderItemProps.ORDER.unwrap(),
                            DissociateAction.DELETE
                    );
                    operator.disconnectExcept(
                            IdPairs.of(
                                    Arrays.asList(
                                            new Tuple2<>(
                                                    Objects.createOrderId(draft -> draft.setX("001").setY("001")),
                                                    Objects.createOrderItemId(draft -> draft.setA(1).setB(1).setC(1))
                                            ),
                                            new Tuple2<>(
                                                    Objects.createOrderId(draft -> draft.setX("001").setY("002")),
                                                    Objects.createOrderItemId(draft -> draft.setA(1).setB(2).setC(1))
                                            )
                                    )
                            )
                    );
                    return operator.ctx.affectedRowCountMap;
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "delete from ORDER_ITEM_PRODUCT_MAPPING tb_1_ " +
                                        "where exists (" +
                                        "--->select * " +
                                        "--->from ORDER_ITEM tb_2_ " +
                                        "--->where " +
                                        "--->--->tb_1_.FK_ORDER_ITEM_A = tb_2_.ORDER_ITEM_A " +
                                        "--->and " +
                                        "--->--->tb_1_.FK_ORDER_ITEM_B = tb_2_.ORDER_ITEM_B " +
                                        "--->and " +
                                        "--->--->tb_1_.FK_ORDER_ITEM_C = tb_2_.ORDER_ITEM_C " +
                                        "--->and " +
                                        "--->--->(tb_2_.FK_ORDER_X, tb_2_.FK_ORDER_Y) in ((?, ?), (?, ?)) " +
                                        "--->and " +
                                        "--->--->(" +
                                        "--->--->--->tb_2_.FK_ORDER_X, tb_2_.FK_ORDER_Y, " +
                                        "--->--->--->tb_2_.ORDER_ITEM_A, tb_2_.ORDER_ITEM_B, tb_2_.ORDER_ITEM_C" +
                                        "--->--->) not in ((?, ?, ?, ?, ?), (?, ?, ?, ?, ?))" +
                                        ")"
                        );
                        it.variables(
                                "001", "001", "001", "002",
                                "001", "001", 1, 1, 1,
                                "001", "002", 1, 2, 1
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from ORDER_ITEM " +
                                        "where " +
                                        "--->(FK_ORDER_X, FK_ORDER_Y) in ((?, ?), (?, ?)) " +
                                        "and " +
                                        "--->(FK_ORDER_X, FK_ORDER_Y, ORDER_ITEM_A, ORDER_ITEM_B, ORDER_ITEM_C) " +
                                        "--->not in ((?, ?, ?, ?, ?), (?, ?, ?, ?, ?))"
                        );
                        it.variables(
                                "001", "001", "001", "002",
                                "001", "001", 1, 1, 1,
                                "001", "002", 1, 2, 1
                        );
                    });
                    ctx.value(map -> {
                        Assertions.assertEquals(2, map.size());
                        Assertions.assertEquals(4, map.get(AffectedTable.of(OrderItemProps.PRODUCTS)));
                        Assertions.assertEquals(2, map.get(AffectedTable.of(OrderItem.class)));
                    });
                }
        );
    }

    @Test
    public void testDisconnectTreeExcept() {
        connectAndExpect(
                con -> {
                    ChildTableOperator operator = operator(
                            getSqlClient(it -> it.setMaxCommandJoinCount(4)),
                            con,
                            ProvinceProps.COUNTRY.unwrap(),
                            DissociateAction.DELETE
                    );
                    operator.disconnectExcept(
                            IdPairs.of(
                                    new Tuple2<>("China", 2),
                                    new Tuple2<>("USA", 4)
                            )
                    );
                    return operator.ctx.affectedRowCountMap;
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "delete from COMPANY tb_1_ " +
                                        "where exists(" +
                                        "--->select * " +
                                        "--->from STREET tb_2_ " +
                                        "--->inner join CITY tb_3_ on tb_2_.CITY_ID = tb_3_.ID " +
                                        "--->inner join PROVINCE tb_4_ on tb_3_.PROVINCE_ID = tb_4_.ID " +
                                        "--->where " +
                                        "--->--->tb_1_.STREET_ID = tb_2_.ID " +
                                        "--->and " +
                                        "--->--->tb_4_.COUNTRY_ID in (?, ?) " +
                                        "--->and " +
                                        "--->--->(tb_4_.COUNTRY_ID, tb_4_.ID) not in ((?, ?), (?, ?))" +
                                        ")"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from STREET tb_1_ " +
                                        "where exists(" +
                                        "--->select * " +
                                        "--->from CITY tb_2_ " +
                                        "--->inner join PROVINCE tb_3_ on tb_2_.PROVINCE_ID = tb_3_.ID " +
                                        "--->where " +
                                        "--->--->tb_1_.CITY_ID = tb_2_.ID " +
                                        "--->and " +
                                        "--->--->tb_3_.COUNTRY_ID in (?, ?) " +
                                        "--->and " +
                                        "--->--->(tb_3_.COUNTRY_ID, tb_3_.ID) not in ((?, ?), (?, ?))" +
                                        ")"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from CITY tb_1_ " +
                                        "where exists(" +
                                        "--->select * " +
                                        "--->from PROVINCE tb_2_ " +
                                        "--->where " +
                                        "--->--->tb_1_.PROVINCE_ID = tb_2_.ID " +
                                        "--->and " +
                                        "--->--->tb_2_.COUNTRY_ID in (?, ?) " +
                                        "--->and " +
                                        "--->--->(tb_2_.COUNTRY_ID, tb_2_.ID) not in ((?, ?), (?, ?))" +
                                        ")"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from PROVINCE " +
                                        "where COUNTRY_ID in (?, ?) and (COUNTRY_ID, ID) not in ((?, ?), (?, ?))"
                        );
                    });
                    ctx.value(map -> {
                        Assertions.assertEquals(3, map.size());
                        Assertions.assertEquals(16, map.get(AffectedTable.of(Street.class)));
                        Assertions.assertEquals(8, map.get(AffectedTable.of(City.class)));
                        Assertions.assertEquals(4, map.get(AffectedTable.of(Province.class)));
                    });
                }
        );
    }
}
