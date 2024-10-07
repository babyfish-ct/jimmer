package org.babyfish.jimmer.sql.embedded;

import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.impl.mutation.QueryReason;
import org.babyfish.jimmer.sql.ast.mutation.AffectedTable;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.dialect.H2Dialect;
import org.babyfish.jimmer.sql.model.embedded.*;
import org.junit.jupiter.api.Test;

public class CommandWhenTupleIsNotSupportedTest extends AbstractMutationTest {

    @Override
    protected JSqlClient getSqlClient() {
        return super.getSqlClient(
                it -> it.setDialect(
                        new H2Dialect() {
                            @Override
                            public boolean isTupleSupported() {
                                return false;
                            }
                        }
                )
        );
    }

    @Test
    public void testSaveOneToManyWitCascadeSetNull() {
        executeAndExpectResult(
                getSqlClient()
                        .getEntities()
                        .saveCommand(
                                OrderDraft.$.produce(order -> {
                                    order
                                            .applyId(id -> id.setX("001").setY("001"))
                                            .setName("new-order-1")
                                            .addIntoOrderItems(item ->
                                                    item
                                                            .applyId(id -> id.setA(1).setB(1).setC(1))
                                                            .setName("order-item-1-1")
                                            )
                                            .addIntoOrderItems(item ->
                                                    item
                                                            .applyId(id -> id.setA(1).setB(1).setC(3))
                                                            .setName("order-item-1-3")
                                            );
                                })
                        )
                        .setDissociateAction(OrderItemProps.ORDER, DissociateAction.SET_NULL),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into ORDER_(" +
                                        "--->ORDER_X, ORDER_Y, NAME" +
                                        ") key(ORDER_X, ORDER_Y) values(?, ?, ?)"
                        );
                        it.variables("001", "001", "new-order-1");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select " +
                                        "--->tb_1_.ORDER_ITEM_A, tb_1_.ORDER_ITEM_B, tb_1_.ORDER_ITEM_C, " +
                                        "--->tb_1_.FK_ORDER_X, tb_1_.FK_ORDER_Y " +
                                        "from ORDER_ITEM tb_1_ " +
                                        "where (" +
                                        "--->--->tb_1_.ORDER_ITEM_A = ? and tb_1_.ORDER_ITEM_B = ? and tb_1_.ORDER_ITEM_C = ? " +
                                        "--->or " +
                                        "--->--->tb_1_.ORDER_ITEM_A = ? and tb_1_.ORDER_ITEM_B = ? and tb_1_.ORDER_ITEM_C = ?" +
                                        ")"
                        );
                        it.variables(1, 1, 1, 1, 1, 3);
                        it.queryReason(QueryReason.TARGET_NOT_TRANSFERABLE);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into ORDER_ITEM(" +
                                        "--->ORDER_ITEM_A, ORDER_ITEM_B, ORDER_ITEM_C, NAME, FK_ORDER_X, FK_ORDER_Y" +
                                        ") values(?, ?, ?, ?, ?, ?)"
                        );
                        it.variables(1, 1, 3, "order-item-1-3", "001", "001");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update ORDER_ITEM set NAME = ?, FK_ORDER_X = ?, FK_ORDER_Y = ? " +
                                        "where ORDER_ITEM_A = ? and ORDER_ITEM_B = ? and ORDER_ITEM_C = ?"
                        );
                        it.variables("order-item-1-1", "001", "001", 1, 1, 1);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update ORDER_ITEM " +
                                        "set FK_ORDER_X = null, FK_ORDER_Y = null " +
                                        "where FK_ORDER_X = ? and FK_ORDER_Y = ? and " +
                                        "--->(ORDER_ITEM_A <> ? or ORDER_ITEM_B <> ? or ORDER_ITEM_C <> ?) " +
                                        "and " +
                                        "--->(ORDER_ITEM_A <> ? or ORDER_ITEM_B <> ? or ORDER_ITEM_C <> ?)"
                        );
                        it.variables("001", "001", 1, 1, 1, 1, 1, 3);
                    });
                    ctx.totalRowCount(4);
                    ctx.rowCount(AffectedTable.of(Order.class), 1);
                    ctx.rowCount(AffectedTable.of(OrderItem.class), 3);
                    ctx.entity(it -> {});
                }
        );
    }

    @Test
    public void saveOneToManyWithCascadeDelete() {
        executeAndExpectResult(
                getSqlClient()
                        .getEntities()
                        .saveCommand(
                                OrderDraft.$.produce(order -> {
                                    order
                                            .applyId(id -> id.setX("001").setY("001"))
                                            .setName("new-order-1")
                                            .addIntoOrderItems(item ->
                                                    item
                                                            .applyId(id -> id.setA(1).setB(1).setC(1))
                                                            .setName("order-item-1-1")
                                            )
                                            .addIntoOrderItems(item ->
                                                    item
                                                            .applyId(id -> id.setA(1).setB(1).setC(3))
                                                            .setName("order-item-1-3")
                                            );
                                })
                        )
                        .setDissociateAction(OrderItemProps.ORDER, DissociateAction.DELETE),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into ORDER_(" +
                                        "--->ORDER_X, ORDER_Y, NAME" +
                                        ") key(" +
                                        "--->ORDER_X, ORDER_Y" +
                                        ") values(?, ?, ?)"
                        );
                        it.variables("001", "001", "new-order-1");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select " +
                                        "--->tb_1_.ORDER_ITEM_A, tb_1_.ORDER_ITEM_B, tb_1_.ORDER_ITEM_C, " +
                                        "--->tb_1_.FK_ORDER_X, tb_1_.FK_ORDER_Y " +
                                        "from ORDER_ITEM tb_1_ " +
                                        "where (" +
                                        "--->--->tb_1_.ORDER_ITEM_A = ? and tb_1_.ORDER_ITEM_B = ? and tb_1_.ORDER_ITEM_C = ? " +
                                        "--->or " +
                                        "--->--->tb_1_.ORDER_ITEM_A = ? and tb_1_.ORDER_ITEM_B = ? and tb_1_.ORDER_ITEM_C = ?" +
                                        ")"
                        );
                        it.variables(1, 1, 1, 1, 1, 3);
                        it.queryReason(QueryReason.TARGET_NOT_TRANSFERABLE);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into ORDER_ITEM(" +
                                        "--->ORDER_ITEM_A, ORDER_ITEM_B, ORDER_ITEM_C, " +
                                        "--->NAME, " +
                                        "--->FK_ORDER_X, FK_ORDER_Y" +
                                        ") values(?, ?, ?, ?, ?, ?)"
                        );
                        it.variables(1, 1, 3, "order-item-1-3", "001", "001");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update ORDER_ITEM set NAME = ?, FK_ORDER_X = ?, FK_ORDER_Y = ? " +
                                        "where ORDER_ITEM_A = ? and ORDER_ITEM_B = ? and ORDER_ITEM_C = ?"
                        );
                        it.variables("order-item-1-1", "001", "001", 1, 1, 1);
                    });
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
                                        "--->--->tb_2_.FK_ORDER_X = ? " +
                                        "--->and " +
                                        "--->--->tb_2_.FK_ORDER_Y = ? " +
                                        "--->and (" +
                                        "--->--->--->tb_2_.ORDER_ITEM_A <> ? " +
                                        "--->--->or " +
                                        "--->--->--->tb_2_.ORDER_ITEM_B <> ? " +
                                        "--->--->or " +
                                        "--->--->--->tb_2_.ORDER_ITEM_C <> ?" +
                                        "--->) and (" +
                                        "--->--->--->tb_2_.ORDER_ITEM_A <> ? " +
                                        "--->--->or " +
                                        "--->--->--->tb_2_.ORDER_ITEM_B <> ? " +
                                        "--->--->or " +
                                        "--->--->--->tb_2_.ORDER_ITEM_C <> ?" +
                                        "--->)" +
                                        ")"
                        );
                        it.variables("001", "001", 1, 1, 1, 1, 1, 3);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from ORDER_ITEM " +
                                        "where " +
                                        "--->FK_ORDER_X = ? and FK_ORDER_Y = ? " +
                                        "and " +
                                        "--->(ORDER_ITEM_A <> ? or ORDER_ITEM_B <> ? or ORDER_ITEM_C <> ?) " +
                                        "and " +
                                        "--->(ORDER_ITEM_A <> ? or ORDER_ITEM_B <> ? or ORDER_ITEM_C <> ?)"
                        );
                        it.variables("001", "001", 1, 1, 1, 1, 1, 3);
                    });
                    ctx.totalRowCount(6);
                    ctx.rowCount(AffectedTable.of(Order.class), 1);
                    ctx.rowCount(AffectedTable.of(OrderItem.class), 3);
                    ctx.rowCount(AffectedTable.of(OrderItemProps.PRODUCTS), 2);
                    ctx.entity(it -> {});
                }
        );
    }

    @Test
    public void setManyToOne() {
        executeAndExpectResult(
                getSqlClient()
                        .getEntities()
                        .saveCommand(
                                OrderItemDraft.$.produce(item ->
                                        item
                                                .setId(OrderItemIdDraft.$.produce(id -> id.setA(1).setB(1).setC(1)))
                                                .setName("order-item-1-1")
                                                .applyOrder(order ->
                                                        order
                                                                .setId(OrderIdDraft.$.produce(id -> id.setX("001").setY("002")))
                                                                .setName("order-2")
                                                )
                                )
                        ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into ORDER_(" +
                                        "--->ORDER_X, ORDER_Y, NAME" +
                                        ") key(" +
                                        "--->ORDER_X, ORDER_Y" +
                                        ") values(?, ?, ?)"
                        );
                        it.variables("001", "002", "order-2");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "merge into ORDER_ITEM(" +
                                        "--->ORDER_ITEM_A, ORDER_ITEM_B, ORDER_ITEM_C, " +
                                        "--->NAME, " +
                                        "--->FK_ORDER_X, FK_ORDER_Y" +
                                        ") key(" +
                                        "--->ORDER_ITEM_A, ORDER_ITEM_B, ORDER_ITEM_C" +
                                        ") values(?, ?, ?, ?, ?, ?)"
                        );
                        it.variables(1, 1, 1, "order-item-1-1", "001", "002");
                    });
                    ctx.totalRowCount(2);
                    ctx.rowCount(AffectedTable.of(Order.class), 1);
                    ctx.rowCount(AffectedTable.of(OrderItem.class), 1);
                    ctx.entity(it -> {});
                }
        );
    }

    @Test
    public void setManyToMany() {
        executeAndExpectResult(
                getSqlClient()
                        .getEntities()
                        .saveCommand(
                                OrderItemDraft.$.produce(item ->
                                        item
                                                .setId(OrderItemIdDraft.$.produce(id -> id.setA(1).setB(1).setC(1)))
                                                .setName("order-item-1-1")
                                                .addIntoProducts(product ->
                                                        product
                                                                .setId(ProductIdDraft.$.produce(id -> id.setAlpha("00A").setBeta("00B")))
                                                                .setName("Boat")
                                                )
                                                .addIntoProducts(product ->
                                                        product
                                                                .setId(ProductIdDraft.$.produce(id -> id.setAlpha("00A").setBeta("00C")))
                                                                .setName("Bus")
                                                )
                                )
                        ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into ORDER_ITEM(" +
                                        "--->ORDER_ITEM_A, ORDER_ITEM_B, ORDER_ITEM_C, NAME" +
                                        ") key(" +
                                        "--->ORDER_ITEM_A, ORDER_ITEM_B, ORDER_ITEM_C" +
                                        ") values(?, ?, ?, ?)"
                        );
                        it.variables(1, 1, 1, "order-item-1-1");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "merge into PRODUCT(" +
                                        "--->PRODUCT_ALPHA, PRODUCT_BETA, NAME" +
                                        ") key(" +
                                        "--->PRODUCT_ALPHA, PRODUCT_BETA" +
                                        ") values(?, ?, ?)"
                        );
                        it.batchVariables(0,"00A", "00B", "Boat");
                        it.batchVariables(1, "00A", "00C", "Bus");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from ORDER_ITEM_PRODUCT_MAPPING " +
                                        "where " +
                                        "--->FK_ORDER_ITEM_A = ? and FK_ORDER_ITEM_B = ? and FK_ORDER_ITEM_C = ? " +
                                        "and " +
                                        "--->(FK_PRODUCT_ALPHA <> ? or FK_PRODUCT_BETA <> ?) " +
                                        "and " +
                                        "--->(FK_PRODUCT_ALPHA <> ? or FK_PRODUCT_BETA <> ?)"
                        );
                        it.variables(
                                1, 1, 1,
                                "00A", "00B",
                                "00A", "00C"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "merge into ORDER_ITEM_PRODUCT_MAPPING tb_1_ " +
                                        "using(values(?, ?, ?, ?, ?)) tb_2_(" +
                                        "--->FK_ORDER_ITEM_A, FK_ORDER_ITEM_B, FK_ORDER_ITEM_C, " +
                                        "--->FK_PRODUCT_ALPHA, FK_PRODUCT_BETA" +
                                        ") " +
                                        "on tb_1_.FK_ORDER_ITEM_A = tb_2_.FK_ORDER_ITEM_A and " +
                                        "--->tb_1_.FK_ORDER_ITEM_B = tb_2_.FK_ORDER_ITEM_B and " +
                                        "--->tb_1_.FK_ORDER_ITEM_C = tb_2_.FK_ORDER_ITEM_C and " +
                                        "--->tb_1_.FK_PRODUCT_ALPHA = tb_2_.FK_PRODUCT_ALPHA and " +
                                        "--->tb_1_.FK_PRODUCT_BETA = tb_2_.FK_PRODUCT_BETA " +
                                        "when not matched then insert(" +
                                        "--->FK_ORDER_ITEM_A, FK_ORDER_ITEM_B, " +
                                        "--->FK_ORDER_ITEM_C, FK_PRODUCT_ALPHA, FK_PRODUCT_BETA" +
                                        ") values(" +
                                        "--->tb_2_.FK_ORDER_ITEM_A, tb_2_.FK_ORDER_ITEM_B, tb_2_.FK_ORDER_ITEM_C, " +
                                        "--->tb_2_.FK_PRODUCT_ALPHA, tb_2_.FK_PRODUCT_BETA" +
                                        ")"
                        );
                        it.batchVariables(0, 1, 1, 1, "00A", "00B");
                        it.batchVariables(1, 1, 1, 1, "00A", "00C");
                    });
                    ctx.totalRowCount(7);
                    ctx.rowCount(AffectedTable.of(OrderItem.class), 1);
                    ctx.rowCount(AffectedTable.of(Product.class), 2);
                    ctx.rowCount(AffectedTable.of(OrderItemProps.PRODUCTS), 4);
                    ctx.entity(it -> {});
                }
        );
    }

    @Test
    public void testSaveInverseManyToMany() {
        executeAndExpectResult(
                getSqlClient()
                        .getEntities()
                        .saveCommand(
                                ProductDraft.$.produce(product ->
                                        product
                                                .applyId(id -> id.setAlpha("00A").setBeta("00A"))
                                                .setName("Car")
                                                .addIntoOrderItems(item ->
                                                        item.applyId(id -> id.setA(1).setB(2).setC(1))
                                                )
                                )
                        ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into PRODUCT(PRODUCT_ALPHA, PRODUCT_BETA, NAME) key(" +
                                        "--->PRODUCT_ALPHA, PRODUCT_BETA" +
                                        ") values(?, ?, ?)"
                        );
                        it.variables("00A", "00A", "Car");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from ORDER_ITEM_PRODUCT_MAPPING " +
                                        "where " +
                                        "--->FK_PRODUCT_ALPHA = ? and FK_PRODUCT_BETA = ? " +
                                        "and " +
                                        "--->(FK_ORDER_ITEM_A <> ? or FK_ORDER_ITEM_B <> ? or FK_ORDER_ITEM_C <> ?)"
                        );
                        it.batchVariables(0, "00A", "00A", 1, 2, 1);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "merge into ORDER_ITEM_PRODUCT_MAPPING tb_1_ " +
                                        "using(values(?, ?, ?, ?, ?)) tb_2_(" +
                                        "--->FK_PRODUCT_ALPHA, FK_PRODUCT_BETA, " +
                                        "--->FK_ORDER_ITEM_A, FK_ORDER_ITEM_B, FK_ORDER_ITEM_C" +
                                        ") " +
                                        "on tb_1_.FK_PRODUCT_ALPHA = tb_2_.FK_PRODUCT_ALPHA and " +
                                        "--->tb_1_.FK_PRODUCT_BETA = tb_2_.FK_PRODUCT_BETA and " +
                                        "--->tb_1_.FK_ORDER_ITEM_A = tb_2_.FK_ORDER_ITEM_A and " +
                                        "--->tb_1_.FK_ORDER_ITEM_B = tb_2_.FK_ORDER_ITEM_B and " +
                                        "--->tb_1_.FK_ORDER_ITEM_C = tb_2_.FK_ORDER_ITEM_C " +
                                        "when not matched then insert(" +
                                        "--->FK_PRODUCT_ALPHA, FK_PRODUCT_BETA, " +
                                        "--->FK_ORDER_ITEM_A, FK_ORDER_ITEM_B, FK_ORDER_ITEM_C" +
                                        ") values(" +
                                        "--->tb_2_.FK_PRODUCT_ALPHA, tb_2_.FK_PRODUCT_BETA, " +
                                        "--->tb_2_.FK_ORDER_ITEM_A, tb_2_.FK_ORDER_ITEM_B, tb_2_.FK_ORDER_ITEM_C" +
                                        ")"
                        );
                        it.variables("00A", "00A", 1, 2, 1);
                    });
                    ctx.totalRowCount(4);
                    ctx.rowCount(AffectedTable.of(Product.class), 1);
                    ctx.rowCount(AffectedTable.of(ProductProps.ORDER_ITEMS), 3);
                    ctx.entity(it -> {});
                }
        );
    }

    @Test
    public void deleteOrderWithCascadeSetNull() {
        executeAndExpectResult(
                getSqlClient()
                        .getEntities()
                        .deleteCommand(
                                Order.class,
                                OrderIdDraft.$.produce(id -> id.setX("001").setY("001"))
                        )
                        .setDissociateAction(OrderItemProps.ORDER, DissociateAction.SET_NULL),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update ORDER_ITEM " +
                                        "set FK_ORDER_X = null, FK_ORDER_Y = null " +
                                        "where FK_ORDER_X = ? and FK_ORDER_Y = ?"
                        );
                        it.variables("001", "001");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from ORDER_ " +
                                        "where ORDER_X = ? and ORDER_Y = ?"
                        );
                        it.variables("001", "001");
                    });
                    ctx.totalRowCount(3);
                    ctx.rowCount(AffectedTable.of(OrderItem.class), 2);
                    ctx.rowCount(AffectedTable.of(Order.class), 1);
                }
        );
    }

    @Test
    public void deleteOrderWithCascadeDelete() {
        executeAndExpectResult(
                getSqlClient()
                        .getEntities()
                        .deleteCommand(
                                Order.class,
                                OrderIdDraft.$.produce(id -> id.setX("001").setY("001"))
                        )
                        .setDissociateAction(
                                OrderItemProps.ORDER,
                                DissociateAction.DELETE
                        ),
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
                                        "--->--->tb_2_.FK_ORDER_X = ? " +
                                        "--->and " +
                                        "--->--->tb_2_.FK_ORDER_Y = ?" +
                                        ")"
                        );
                        it.variables("001", "001");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from ORDER_ITEM " +
                                        "where FK_ORDER_X = ? and FK_ORDER_Y = ?"
                        );
                        it.variables("001", "001");
                    });
                    ctx.statement(it -> {
                        it.sql("delete from ORDER_ where ORDER_X = ? and ORDER_Y = ?");
                        it.variables("001", "001");
                    });
                    ctx.rowCount(AffectedTable.of(OrderItemProps.PRODUCTS), 4);
                    ctx.rowCount(AffectedTable.of(OrderItem.class), 2);
                    ctx.rowCount(AffectedTable.of(Order.class), 1);
                }
        );
    }
}
