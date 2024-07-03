package org.babyfish.jimmer.sql.ast.impl.mutation.save;

import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.dialect.H2Dialect;
import org.babyfish.jimmer.sql.event.TriggerType;
import org.babyfish.jimmer.sql.model.BookProps;
import org.babyfish.jimmer.sql.model.Objects;
import org.babyfish.jimmer.sql.model.embedded.OrderItemProps;
import org.babyfish.jimmer.sql.model.flat.ProvinceProps;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import static org.babyfish.jimmer.sql.common.Constants.*;
import static org.babyfish.jimmer.sql.common.Constants.graphQLInActionId1;

public class ModifyBySelectChildTest extends AbstractChildOperatorTest {

    @Test
    public void testDisconnectExceptBySimpleInPredicate() {
        connectAndExpect(
                con -> {
                    return operator(
                            getSqlClient(it -> it.setTriggerType(TriggerType.TRANSACTION_ONLY)),
                            con,
                            BookProps.STORE.unwrap()
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
                                "select tb_1_.STORE_ID, tb_1_.ID " +
                                        "from BOOK tb_1_ " +
                                        "where tb_1_.STORE_ID = ? and tb_1_.ID not in (?, ?)"
                        );
                        it.variables(manningId, graphQLInActionId1, graphQLInActionId2);
                    });
                    ctx.statement(it -> {
                        it.sql("update BOOK set STORE_ID = null where ID = ?");
                        it.variables(graphQLInActionId3);
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
                            getSqlClient(it -> it.setTriggerType(TriggerType.TRANSACTION_ONLY)),
                            con,
                            BookProps.STORE.unwrap()
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
                    ctx.statement(it -> {
                        it.sql(
                                "update BOOK " +
                                        "set STORE_ID = null " +
                                        "where ID in (?, ?, ?, ?, ?, ?, ?, ?)"
                        );
                        it.unorderedVariables(
                                learningGraphQLId2, learningGraphQLId3,
                                effectiveTypeScriptId2, effectiveTypeScriptId3,
                                programmingTypeScriptId2, programmingTypeScriptId3,
                                graphQLInActionId2, graphQLInActionId3
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
                            getSqlClient(it -> {
                                it.setDialect(new H2Dialect());
                                it.setTriggerType(TriggerType.TRANSACTION_ONLY);
                            }),
                            con,
                            BookProps.STORE.unwrap()
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
                                "select tb_1_.STORE_ID, tb_1_.ID " +
                                        "from BOOK tb_1_ " +
                                        "where tb_1_.STORE_ID = any(?) and " +
                                        "(tb_1_.STORE_ID, tb_1_.ID) not in ((?, ?), (?, ?), (?, ?), (?, ?))"
                        );
                        it.variables(
                                new Object[]{oreillyId, manningId},
                                oreillyId, learningGraphQLId1,
                                oreillyId, effectiveTypeScriptId1,
                                oreillyId, programmingTypeScriptId1,
                                manningId, graphQLInActionId1
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update BOOK set STORE_ID = null where ID = any(?)"
                        );
                        it.variables(list -> {
                            Assertions.assertEquals(1, list.size());
                            Assertions.assertEquals(
                                    new HashSet<>(
                                            Arrays.asList(
                                                    learningGraphQLId2, learningGraphQLId3,
                                                    effectiveTypeScriptId2, effectiveTypeScriptId3,
                                                    programmingTypeScriptId2, programmingTypeScriptId3,
                                                    graphQLInActionId2, graphQLInActionId3
                                            )
                                    ),
                                    new HashSet<>(
                                            (Collection<?>) list.get(0)
                                    )
                            );
                        });
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
                            getSqlClient(it -> it.setTriggerType(TriggerType.TRANSACTION_ONLY)),
                            con,
                            OrderItemProps.ORDER.unwrap()
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
                                "select " +
                                        "--->tb_1_.FK_ORDER_X, tb_1_.FK_ORDER_Y, " +
                                        "--->tb_1_.ORDER_ITEM_A, tb_1_.ORDER_ITEM_B, tb_1_.ORDER_ITEM_C " +
                                        "from ORDER_ITEM tb_1_ where " +
                                        "--->(tb_1_.FK_ORDER_X, tb_1_.FK_ORDER_Y) = (?, ?) " +
                                        "and " +
                                        "--->(tb_1_.ORDER_ITEM_A, tb_1_.ORDER_ITEM_B, tb_1_.ORDER_ITEM_C) <> (?, ?, ?)"
                        );
                        it.variables(
                                "001", "001", 1, 1, 1
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from ORDER_ITEM " +
                                        "where (ORDER_ITEM_A, ORDER_ITEM_B, ORDER_ITEM_C) = (?, ?, ?)"
                        );
                        it.variables(1, 1, 2);
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
                            getSqlClient(it -> it.setTriggerType(TriggerType.TRANSACTION_ONLY)),
                            con,
                            OrderItemProps.ORDER.unwrap()
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
                                        "--->) not in ((?, ?, ?, ?, ?), (?, ?, ?, ?, ?)" +
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
                                        "where (" +
                                        "--->ORDER_ITEM_A, ORDER_ITEM_B, ORDER_ITEM_C" +
                                        ") in ((?, ?, ?), (?, ?, ?))"
                        );
                        it.variables(
                                1, 1, 2,
                                2, 1, 1
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
                            getSqlClient(it -> it.setTriggerType(TriggerType.TRANSACTION_ONLY)),
                            con,
                            ProvinceProps.COUNTRY.unwrap(),
                            DissociateAction.SET_NULL
                    ).disconnectExcept(
                            IdPairs.of(
                                    new Tuple2<>(1L, 2L),
                                    new Tuple2<>(2L, 4L)
                            )
                    );
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.COUNTRY_ID, tb_1_.ID " +
                                        "from PROVINCE tb_1_ " +
                                        "where " +
                                        "--->tb_1_.COUNTRY_ID in (?, ?) " +
                                        "and " +
                                        "--->(tb_1_.COUNTRY_ID, tb_1_.ID) not in ((?, ?), (?, ?))"
                        );
                        it.variables(1L, 2L, 1L, 2L, 2L, 4L);
                    });
                }
        );
    }
}
