package org.babyfish.jimmer.sql.ast.impl.mutation.save;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.dialect.H2Dialect;
import org.babyfish.jimmer.sql.model.*;
import org.babyfish.jimmer.sql.model.embedded.OrderProps;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static org.babyfish.jimmer.sql.common.Constants.*;

public class ChildTableOperatorTest extends AbstractMutationTest {

    @Test
    public void testFindDisconnectExceptIdPairsByOneSource() {
        connectAndExpect(
                con -> {
                    return operator(
                            getSqlClient(),
                            con,
                            BookStoreProps.BOOKS.unwrap()
                    ).findDisconnectingIdPairs(
                            IdPairs.of(
                                    Collections.singleton(
                                            new Tuple2<>(manningId, graphQLInActionId1)
                                    )
                            )
                    );
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.STORE_ID, tb_1_.ID " +
                                        "from BOOK tb_1_ " +
                                        "where " +
                                        "--->tb_1_.STORE_ID = ? " +
                                        "and " +
                                        "--->tb_1_.ID <> ?"
                        );
                        it.variables(manningId, graphQLInActionId1);
                    });
                    ctx.value(idPairs -> {
                        Assertions.assertEquals(
                                new HashSet<>(
                                        Arrays.asList(
                                                new Tuple2<>(manningId, graphQLInActionId2),
                                                new Tuple2<>(manningId, graphQLInActionId3)
                                        )
                                ),
                                new HashSet<>(idPairs.tuples())
                        );
                    });
                }
        );
    }

    @Test
    public void testFindDisconnectExceptIdPairsByMultiSources() {
        connectAndExpect(
                con -> {
                    return operator(
                            getSqlClient(),
                            con,
                            BookStoreProps.BOOKS.unwrap()
                    ).findDisconnectingIdPairs(
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
                                "select tb_1_.STORE_ID, tb_1_.ID " +
                                        "from BOOK tb_1_ " +
                                        "where " +
                                        "--->tb_1_.STORE_ID in (?, ?) " +
                                        "and " +
                                        "--->(tb_1_.STORE_ID, tb_1_.ID) not in ((?, ?), (?, ?), (?, ?), (?, ?))"
                        );
                        it.variables(
                                oreillyId, manningId,
                                oreillyId, learningGraphQLId1,
                                oreillyId, effectiveTypeScriptId1,
                                oreillyId, programmingTypeScriptId1,
                                manningId, graphQLInActionId1
                        );
                    });
                    ctx.value(idPairs -> {
                        Assertions.assertEquals(
                                new HashSet<>(
                                    Arrays.asList(
                                            new Tuple2<>(oreillyId, learningGraphQLId2),
                                            new Tuple2<>(oreillyId, learningGraphQLId3),
                                            new Tuple2<>(oreillyId, effectiveTypeScriptId2),
                                            new Tuple2<>(oreillyId, effectiveTypeScriptId3),
                                            new Tuple2<>(oreillyId, programmingTypeScriptId2),
                                            new Tuple2<>(oreillyId, programmingTypeScriptId3),
                                            new Tuple2<>(manningId, graphQLInActionId2),
                                            new Tuple2<>(manningId, graphQLInActionId3)
                                    )
                                ),
                                new HashSet<>(idPairs.tuples())
                        );
                    });
                }
        );
    }

    @Test
    public void testDisconnectExceptBySimpleInPredicate() {
        connectAndExpect(
                con -> {
                    return operator(
                            getSqlClient(),
                            con,
                            BookStoreProps.BOOKS.unwrap()
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
                            BookStoreProps.BOOKS.unwrap()
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
                            BookStoreProps.BOOKS.unwrap()
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
    public void testFindDisconnectExceptIdPairsByOneEmbeddedSource() {
        connectAndExpect(
                con -> {
                    return operator(
                            getSqlClient(),
                            con,
                            OrderProps.ORDER_ITEMS.unwrap()
                    ).findDisconnectingIdPairs(
                            IdPairs.of(
                                    Arrays.asList(
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
                                "select " +
                                        "--->tb_1_.FK_ORDER_X, tb_1_.FK_ORDER_Y, " +
                                        "--->tb_1_.ORDER_ITEM_A, tb_1_.ORDER_ITEM_B, tb_1_.ORDER_ITEM_C " +
                                        "from ORDER_ITEM tb_1_ " +
                                        "where " +
                                        "--->(tb_1_.FK_ORDER_X, tb_1_.FK_ORDER_Y) = (?, ?) " +
                                        "and " +
                                        "--->(tb_1_.ORDER_ITEM_A, tb_1_.ORDER_ITEM_B, tb_1_.ORDER_ITEM_C) <> (?, ?, ?)"
                        );
                        it.variables(
                                "001", "001", 1, 1, 1
                        );
                    });
                    ctx.value(
                            "TupleIdPairs[" +
                                    "--->Tuple2(_1={\"x\":\"001\",\"y\":\"001\"}, _2={\"a\":1,\"b\":1,\"c\":2})" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testFindDisconnectExceptIdPairsByMultiEmbeddedSources() {
        connectAndExpect(
                con -> {
                    return operator(
                            getSqlClient(),
                            con,
                            OrderProps.ORDER_ITEMS.unwrap()
                    ).findDisconnectingIdPairs(
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
                                "select " +
                                        "--->tb_1_.FK_ORDER_X, tb_1_.FK_ORDER_Y, " +
                                        "--->tb_1_.ORDER_ITEM_A, tb_1_.ORDER_ITEM_B, tb_1_.ORDER_ITEM_C " +
                                        "from ORDER_ITEM tb_1_ " +
                                        "where " +
                                        "--->(tb_1_.FK_ORDER_X, tb_1_.FK_ORDER_Y) in ((?, ?), (?, ?)) " +
                                        "and " +
                                        "--->(" +
                                        "--->--->tb_1_.FK_ORDER_X, tb_1_.FK_ORDER_Y, " +
                                        "--->--->tb_1_.ORDER_ITEM_A, tb_1_.ORDER_ITEM_B, tb_1_.ORDER_ITEM_C" +
                                        "--->) " +
                                        "--->not in ((?, ?, ?, ?, ?), (?, ?, ?, ?, ?))"
                        );
                        it.variables(
                                "001", "001", "001", "002",
                                "001", "001", 1, 1, 1,
                                "001", "002", 1, 2, 1
                        );
                    });
                    ctx.value(
                            "TupleIdPairs[" +
                                    "--->Tuple2(_1={\"x\":\"001\",\"y\":\"001\"}, _2={\"a\":1,\"b\":1,\"c\":2}), " +
                                    "--->Tuple2(_1={\"x\":\"001\",\"y\":\"002\"}, _2={\"a\":2,\"b\":1,\"c\":1})" +
                                    "]"
                    );
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
                            OrderProps.ORDER_ITEMS.unwrap()
                    ).disconnectExcept(
                            IdPairs.of(
                                    Arrays.asList(
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
                            OrderProps.ORDER_ITEMS.unwrap()
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

    private static ChildTableOperator operator(
            JSqlClient sqlClient,
            Connection con,
            ImmutableProp oneToManyProp
    ) {
        SaveOptionsImpl options = new SaveOptionsImpl((JSqlClientImplementor) sqlClient);
        return new ChildTableOperator(
                new SaveContext(
                        options,
                        con,
                        oneToManyProp.getDeclaringType()
                ).to(oneToManyProp)
        );
    }
}
