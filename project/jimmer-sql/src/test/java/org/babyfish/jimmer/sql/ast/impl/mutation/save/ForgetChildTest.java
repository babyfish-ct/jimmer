package org.babyfish.jimmer.sql.ast.impl.mutation.save;

import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.ast.mutation.AffectedTable;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.dialect.H2Dialect;
import org.babyfish.jimmer.sql.model.Book;
import org.babyfish.jimmer.sql.model.BookProps;
import org.babyfish.jimmer.sql.model.Objects;
import org.babyfish.jimmer.sql.model.embedded.OrderItem;
import org.babyfish.jimmer.sql.model.embedded.OrderItemProps;
import org.babyfish.jimmer.sql.model.flat.Province;
import org.babyfish.jimmer.sql.model.flat.ProvinceProps;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.babyfish.jimmer.sql.common.Constants.*;
import static org.babyfish.jimmer.sql.common.Constants.graphQLInActionId1;

public class ForgetChildTest extends AbstractChildOperatorTest {

    @Test
    public void testDisconnectExceptBySimpleInPredicate() {
        connectAndExpect(
                con -> {
                    ChildTableOperator operator = operator(
                            getSqlClient(),
                            con,
                            BookProps.STORE.unwrap(),
                            DissociateAction.SET_NULL
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
                                "update BOOK " +
                                        "set STORE_ID = null " +
                                        "where STORE_ID = ? and ID not in (?, ?)"
                        );
                        it.variables(manningId, graphQLInActionId1, graphQLInActionId2);
                    });
                    ctx.value(map -> {
                        Assertions.assertEquals(1, map.size());
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
                            DissociateAction.SET_NULL
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
                                "update BOOK " +
                                        "set STORE_ID = null " +
                                        "where " +
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
                        Assertions.assertEquals(1, map.size());
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
                            DissociateAction.SET_NULL
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
                                "update BOOK " +
                                        "set STORE_ID = null " +
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
                        Assertions.assertEquals(1, map.size());
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
                            DissociateAction.SET_NULL
                    );
                    operator.disconnectExcept(
                            IdPairs.of(
                                    Collections.singletonList(
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
                                "update ORDER_ITEM " +
                                        "set FK_ORDER_X = null, FK_ORDER_Y = null " +
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
                        Assertions.assertEquals(1, map.size());
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
                            DissociateAction.SET_NULL
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
                                "update ORDER_ITEM " +
                                        "set FK_ORDER_X = null, FK_ORDER_Y = null " +
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
                        Assertions.assertEquals(1, map.size());
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
                            getSqlClient(),
                            con,
                            ProvinceProps.COUNTRY.unwrap(),
                            DissociateAction.SET_NULL
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
                                "update PROVINCE set COUNTRY_ID = null " +
                                        "where COUNTRY_ID in (?, ?) and (COUNTRY_ID, ID) not in ((?, ?), (?, ?))"
                        );
                    });
                    ctx.value(map -> {
                        Assertions.assertEquals(1, map.size());
                        Assertions.assertEquals(4, map.get(AffectedTable.of(Province.class)));
                    });
                }
        );
    }
}
