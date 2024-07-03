package org.babyfish.jimmer.sql.ast.impl.mutation.save;

import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.dialect.H2Dialect;
import org.babyfish.jimmer.sql.model.BookProps;
import org.babyfish.jimmer.sql.model.Objects;
import org.babyfish.jimmer.sql.model.embedded.OrderItemProps;
import org.babyfish.jimmer.sql.model.flat.ProvinceProps;
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
                    return operator(
                            getSqlClient(),
                            con,
                            BookProps.STORE.unwrap(),
                            DissociateAction.SET_NULL
                    ).disconnectExcept(
                            IdPairs.of(
                                    Arrays.asList(
                                            new Tuple2<>(manningId, graphQLInActionId1),
                                            new Tuple2<>(manningId, graphQLInActionId2)
                                    )
                            )
                    );
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
                    ctx.value("1");
                }
        );
    }

    @Test
    public void testDisconnectExceptByComplexInPredicate() {
        connectAndExpect(
                con -> {
                    return operator(
                            getSqlClient(),
                            con,
                            BookProps.STORE.unwrap(),
                            DissociateAction.SET_NULL
                    ).disconnectExcept(
                            IdPairs.of(
                                    Arrays.asList(
                                            new Tuple2<>(oreillyId, learningGraphQLId1),
                                            new Tuple2<>(oreillyId, effectiveTypeScriptId1),
                                            new Tuple2<>(oreillyId, programmingTypeScriptId1),
                                            new Tuple2<>(manningId, graphQLInActionId1)
                                    )
                            )
                    );
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
                    ctx.value("8");
                }
        );
    }

    @Test
    public void testDisconnectExceptByBatch() {
        connectAndExpect(
                con -> {
                    return operator(
                            getSqlClient(it -> it.setDialect(new H2Dialect())),
                            con,
                            BookProps.STORE.unwrap(),
                            DissociateAction.SET_NULL
                    ).disconnectExcept(
                            IdPairs.of(
                                    Arrays.asList(
                                            new Tuple2<>(oreillyId, learningGraphQLId1),
                                            new Tuple2<>(oreillyId, effectiveTypeScriptId1),
                                            new Tuple2<>(oreillyId, programmingTypeScriptId1),
                                            new Tuple2<>(manningId, graphQLInActionId1)
                                    )
                            )
                    );
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
                    ctx.value("8");
                }
        );
    }

    @Test
    public void testDisconnectExceptBySimpleInPredicateAndEmbedded() {
        connectAndExpect(
                con -> {
                    return operator(
                            getSqlClient(),
                            con,
                            OrderItemProps.ORDER.unwrap(),
                            DissociateAction.SET_NULL
                    ).disconnectExcept(
                            IdPairs.of(
                                    Collections.singletonList(
                                            new Tuple2<>(
                                                    Objects.createOrderId(draft -> draft.setX("001").setY("001")),
                                                    Objects.createOrderItemId(draft -> draft.setA(1).setB(1).setC(1))
                                            )
                                    )
                            )
                    );
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
                    ctx.value("1");
                }
        );
    }

    @Test
    public void testDisconnectExceptByComplexInPredicateAndEmbedded() {
        connectAndExpect(
                con -> {
                    return operator(
                            getSqlClient(),
                            con,
                            OrderItemProps.ORDER.unwrap(),
                            DissociateAction.SET_NULL
                    ).disconnectExcept(
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
                    ctx.value("2");
                }
        );
    }

    @Test
    public void testDisconnectTreeExcept() {
        connectAndExpect(
                con -> {
                    return operator(
                            getSqlClient(),
                            con,
                            ProvinceProps.COUNTRY.unwrap(),
                            DissociateAction.SET_NULL
                    ).disconnectExcept(
                            IdPairs.of(
                                    new Tuple2<>(1L, 2),
                                    new Tuple2<>(2L, 4)
                            )
                    );
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update PROVINCE set COUNTRY_ID = null " +
                                        "where COUNTRY_ID in (?, ?) and (COUNTRY_ID, ID) not in ((?, ?), (?, ?))"
                        );
                    });
                }
        );
    }
}
