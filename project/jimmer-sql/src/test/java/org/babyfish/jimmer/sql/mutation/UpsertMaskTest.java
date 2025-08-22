package org.babyfish.jimmer.sql.mutation;

import org.babyfish.jimmer.sql.ast.mutation.QueryReason;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.ast.mutation.UpsertMask;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.common.Constants;
import org.babyfish.jimmer.sql.dialect.H2Dialect;
import org.babyfish.jimmer.sql.event.TriggerType;
import org.babyfish.jimmer.sql.model.BookProps;
import org.babyfish.jimmer.sql.model.Immutables;
import org.babyfish.jimmer.sql.model.embedded.OrderIdProps;
import org.babyfish.jimmer.sql.model.embedded.OrderItem;
import org.babyfish.jimmer.sql.model.embedded.OrderItemProps;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.UUID;

public class UpsertMaskTest extends AbstractMutationTest {

    @Test
    public void testOnlyOrder() {
        executeAndExpectResult(
                getSqlClient(it -> it.setDialect(new H2Dialect()))
                        .saveEntitiesCommand(
                                Arrays.asList(
                                        Immutables.createOrderItem(draft -> {
                                            draft.setId(Immutables.createOrderItemId(id -> id.setA(1).setB(2).setC(1)));
                                            draft.setName("order-item-1");
                                            draft.setOrderId(Immutables.createOrderId(id -> id.setX("001").setY("001")));
                                        }),
                                        Immutables.createOrderItem(draft -> {
                                            draft.setId(Immutables.createOrderItemId(id -> id.setA(1).setB(2).setC(2)));
                                            draft.setName("order-item-2");
                                            draft.setOrderId(Immutables.createOrderId(id -> id.setX("001").setY("001")));
                                        })
                                )
                        )
                        .setUpsertMask(OrderItemProps.ORDER),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into ORDER_ITEM tb_1_ " +
                                        "using(values(?, ?, ?, ?, ?, ?)) tb_2_(" +
                                        "--->ORDER_ITEM_A, ORDER_ITEM_B, ORDER_ITEM_C, NAME, FK_ORDER_X, FK_ORDER_Y" +
                                        ") on tb_1_.ORDER_ITEM_A = tb_2_.ORDER_ITEM_A " +
                                        "and " +
                                        "--->tb_1_.ORDER_ITEM_B = tb_2_.ORDER_ITEM_B " +
                                        "and " +
                                        "--->tb_1_.ORDER_ITEM_C = tb_2_.ORDER_ITEM_C " +
                                        "when matched then update set " +
                                        "--->FK_ORDER_X = tb_2_.FK_ORDER_X, FK_ORDER_Y = tb_2_.FK_ORDER_Y " +
                                        "when not matched then " +
                                        "--->insert(" +
                                        "--->--->ORDER_ITEM_A, ORDER_ITEM_B, ORDER_ITEM_C, " +
                                        "--->--->NAME, " +
                                        "--->--->FK_ORDER_X, FK_ORDER_Y" +
                                        "--->) values(" +
                                        "--->--->tb_2_.ORDER_ITEM_A, tb_2_.ORDER_ITEM_B, tb_2_.ORDER_ITEM_C, " +
                                        "--->--->tb_2_.NAME, " +
                                        "--->--->tb_2_.FK_ORDER_X, tb_2_.FK_ORDER_Y" +
                                        "--->)"
                        );
                    });
                    ctx.entity(it -> {});
                    ctx.entity(it -> {});
                }
        );
    }

    @Test
    public void testOnlyOrderX() {
        executeAndExpectResult(
                getSqlClient(it -> it.setDialect(new H2Dialect()))
                        .saveEntitiesCommand(
                                Arrays.asList(
                                        Immutables.createOrderItem(draft -> {
                                            draft.setId(Immutables.createOrderItemId(id -> id.setA(1).setB(2).setC(1)));
                                            draft.setName("order-item-1");
                                            draft.setOrderId(Immutables.createOrderId(id -> id.setX("001").setY("001")));
                                        }),
                                        Immutables.createOrderItem(draft -> {
                                            draft.setId(Immutables.createOrderItemId(id -> id.setA(1).setB(2).setC(2)));
                                            draft.setName("order-item-2");
                                            draft.setOrderId(Immutables.createOrderId(id -> id.setX("001").setY("001")));
                                        })
                                )
                        )
                        .setUpsertMask(
                                UpsertMask.of(OrderItem.class)
                                        .addUpdatablePath(OrderItemProps.ORDER, OrderIdProps.X)
                        ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into ORDER_ITEM tb_1_ " +
                                        "using(values(?, ?, ?, ?, ?, ?)) tb_2_(" +
                                        "--->ORDER_ITEM_A, ORDER_ITEM_B, ORDER_ITEM_C, NAME, FK_ORDER_X, FK_ORDER_Y" +
                                        ") on tb_1_.ORDER_ITEM_A = tb_2_.ORDER_ITEM_A " +
                                        "and " +
                                        "--->tb_1_.ORDER_ITEM_B = tb_2_.ORDER_ITEM_B " +
                                        "and " +
                                        "--->tb_1_.ORDER_ITEM_C = tb_2_.ORDER_ITEM_C " +
                                        "when matched then update set " +
                                        "--->FK_ORDER_X = tb_2_.FK_ORDER_X " +
                                        "when not matched then " +
                                        "--->insert(" +
                                        "--->--->ORDER_ITEM_A, ORDER_ITEM_B, ORDER_ITEM_C, " +
                                        "--->--->NAME, " +
                                        "--->--->FK_ORDER_X, FK_ORDER_Y" +
                                        "--->) values(" +
                                        "--->--->tb_2_.ORDER_ITEM_A, tb_2_.ORDER_ITEM_B, tb_2_.ORDER_ITEM_C, " +
                                        "--->--->tb_2_.NAME, " +
                                        "--->--->tb_2_.FK_ORDER_X, tb_2_.FK_ORDER_Y" +
                                        "--->)"
                        );
                    });
                    ctx.entity(it -> {});
                    ctx.entity(it -> {});
                }
        );
    }

    @Test
    public void testUpsertWithTrigger() {
        UUID graphQLInActionId4 = UUID.fromString("6f08ac6a-58cb-4716-876f-b1c149424e00");
        executeAndExpectResult(
                getSqlClient(it -> {
                  it.setDialect(new H2Dialect()).setTriggerType(TriggerType.TRANSACTION_ONLY);
                  it.setInListToAnyEqualityEnabled(true);
                })
                        .saveEntitiesCommand(
                                Arrays.asList(
                                        Immutables.createBook(draft -> {
                                            draft.setId(Constants.graphQLInActionId3);
                                            draft.setName("GraphQL in Action+");
                                            draft.setEdition(3);
                                            draft.setPrice(new BigDecimal("57.9"));
                                        }),
                                        Immutables.createBook(draft -> {
                                            draft.setId(graphQLInActionId4);
                                            draft.setName("GraphQL in Action");
                                            draft.setEdition(4);
                                            draft.setPrice(new BigDecimal("58.9"));
                                        })
                                )
                        )
                        .setUpsertMask(BookProps.PRICE),
                ctx -> {
                    ctx.statement(it -> {
                        it.queryReason(QueryReason.TRIGGER);
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                        "from BOOK tb_1_ " +
                                        "where tb_1_.ID = any(?)"
                        );
                        it.variables((Object) new Object[] {Constants.graphQLInActionId3, graphQLInActionId4});
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into BOOK(ID, NAME, EDITION, PRICE) " +
                                        "values(?, ?, ?, ?)"
                        );
                        it.variables(graphQLInActionId4, "GraphQL in Action", 4, new BigDecimal("58.9"));
                    });
                    ctx.statement(it -> {
                        it.sql("update BOOK set PRICE = ? where ID = ?");
                        it.variables(new BigDecimal("57.9"), Constants.graphQLInActionId3);
                    });
                    ctx.entity(it -> {});
                    ctx.entity(it -> {});
                }
        );
    }

    @Test
    public void testUpdateIsNotAffected() {
        executeAndExpectResult(
                getSqlClient(it -> it.setDialect(new H2Dialect()))
                        .saveCommand(
                                Immutables.createBook(draft -> {
                                    draft.setId(Constants.graphQLInActionId3);
                                    draft.setName("GraphQL in Action+");
                                    draft.setEdition(4);
                                    draft.setPrice(new BigDecimal("57.9"));
                                })
                        )
                        .setMode(SaveMode.UPDATE_ONLY)
                        .setUpsertMask(BookProps.PRICE),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("update BOOK set NAME = ?, EDITION = ?, PRICE = ? where ID = ?");
                    });
                    ctx.entity(it -> {});
                }
        );
    }
}
