package org.babyfish.jimmer.sql.embedded;

import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.ast.mutation.AffectedTable;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.model.embedded.*;
import org.junit.jupiter.api.Test;

public class CommandTest extends AbstractMutationTest {

    @Test
    public void testSaveOneToManyWitCascadeSetNull() {
        executeAndExpectResult(
                getSqlClient()
                        .getEntities()
                        .saveCommand(
                                OrderDraft.$.produce(order -> {
                                    order
                                            .setId(id -> id.setX("001").setY("001"))
                                            .setName("new-order-1")
                                            .addIntoOrderItems(item ->
                                                    item
                                                            .setId(id -> id.setA(1).setB(1).setC(1))
                                                            .setName("order-item-1-1")
                                            )
                                            .addIntoOrderItems(item ->
                                                    item
                                                            .setId(id -> id.setA(1).setB(1).setC(3))
                                                            .setName("order-item-1-3")
                                            );
                                }),
                                true
                        )
                        .configure(cfg ->
                                cfg.setDissociateAction(OrderItemProps.ORDER, DissociateAction.SET_NULL)
                        ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ORDER_X, tb_1_.ORDER_Y " +
                                        "from ORDER_ as tb_1_ " +
                                        "where (tb_1_.ORDER_X, tb_1_.ORDER_Y) = (?, ?) " +
                                        "for update"
                        );
                        it.variables("001", "001");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update ORDER_ set NAME = ? " +
                                        "where (ORDER_X, ORDER_Y) = (?, ?)"
                        );
                        it.variables("new-order-1", "001", "001");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ORDER_ITEM_A, tb_1_.ORDER_ITEM_B, tb_1_.ORDER_ITEM_C " +
                                        "from ORDER_ITEM as tb_1_ " +
                                        "where (tb_1_.ORDER_ITEM_A, tb_1_.ORDER_ITEM_B, tb_1_.ORDER_ITEM_C) = (?, ?, ?) " +
                                        "for update"
                        );
                        it.variables(1, 1, 1);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update ORDER_ITEM " +
                                        "set NAME = ?, FK_ORDER_X = ?, FK_ORDER_Y = ? " +
                                        "where (ORDER_ITEM_A, ORDER_ITEM_B, ORDER_ITEM_C) = (?, ?, ?)"
                        );
                        it.variables("order-item-1-1", "001", "001", 1, 1, 1);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ORDER_ITEM_A, tb_1_.ORDER_ITEM_B, tb_1_.ORDER_ITEM_C " +
                                        "from ORDER_ITEM as tb_1_ " +
                                        "where (tb_1_.ORDER_ITEM_A, tb_1_.ORDER_ITEM_B, tb_1_.ORDER_ITEM_C) = (?, ?, ?) " +
                                        "for update"
                        );
                        it.variables(1, 1, 3);
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
                                "update ORDER_ITEM " +
                                        "set FK_ORDER_X = null, FK_ORDER_Y = null " +
                                        "where (FK_ORDER_X, FK_ORDER_Y) = (?, ?) and " +
                                        "(" +
                                        "--->ORDER_ITEM_A, ORDER_ITEM_B, ORDER_ITEM_C" +
                                        ") not in (" +
                                        "--->(?, ?, ?), (?, ?, ?)" +
                                        ")"
                        );
                        it.variables("001", "001", 1, 1, 1, 1, 1, 3);
                    });
                    ctx.entity(it -> {

                    });
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
                                            .setId(id -> id.setX("001").setY("001"))
                                            .setName("new-order-1")
                                            .addIntoOrderItems(item ->
                                                    item
                                                            .setId(id -> id.setA(1).setB(1).setC(1))
                                                            .setName("order-item-1-1")
                                            )
                                            .addIntoOrderItems(item ->
                                                    item
                                                            .setId(id -> id.setA(1).setB(1).setC(3))
                                                            .setName("order-item-1-3")
                                            );
                                }),
                                true
                        )
                        .configure(cfg ->
                                cfg.setDissociateAction(OrderItemProps.ORDER, DissociateAction.DELETE)
                        ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ORDER_X, tb_1_.ORDER_Y " +
                                        "from ORDER_ as tb_1_ " +
                                        "where (tb_1_.ORDER_X, tb_1_.ORDER_Y) = (?, ?) for update"
                        );
                        it.variables("001", "001");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update ORDER_ set NAME = ? " +
                                        "where (ORDER_X, ORDER_Y) = (?, ?)"
                        );
                        it.variables("new-order-1", "001", "001");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ORDER_ITEM_A, tb_1_.ORDER_ITEM_B, tb_1_.ORDER_ITEM_C " +
                                        "from ORDER_ITEM as tb_1_ " +
                                        "where (tb_1_.ORDER_ITEM_A, tb_1_.ORDER_ITEM_B, tb_1_.ORDER_ITEM_C) = (?, ?, ?) " +
                                        "for update"
                        );
                        it.variables(1, 1, 1);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update ORDER_ITEM " +
                                        "set NAME = ?, FK_ORDER_X = ?, FK_ORDER_Y = ? " +
                                        "where (ORDER_ITEM_A, ORDER_ITEM_B, ORDER_ITEM_C) = (?, ?, ?)"
                        );
                        it.variables("order-item-1-1", "001", "001", 1, 1, 1);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ORDER_ITEM_A, tb_1_.ORDER_ITEM_B, tb_1_.ORDER_ITEM_C " +
                                        "from ORDER_ITEM as tb_1_ " +
                                        "where (tb_1_.ORDER_ITEM_A, tb_1_.ORDER_ITEM_B, tb_1_.ORDER_ITEM_C) = (?, ?, ?) " +
                                        "for update"
                        );
                        it.variables(1, 1, 3);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into ORDER_ITEM(" +
                                        "--->ORDER_ITEM_A, ORDER_ITEM_B, ORDER_ITEM_C, NAME, FK_ORDER_X, FK_ORDER_Y" +
                                        ") values(" +
                                        "--->?, ?, ?, ?, ?, ?" +
                                        ")"
                        );
                        it.variables(1, 1, 3, "order-item-1-3", "001", "001");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select ORDER_ITEM_A, ORDER_ITEM_B, ORDER_ITEM_C from ORDER_ITEM " +
                                        "where (FK_ORDER_X, FK_ORDER_Y) = (?, ?) and " +
                                        "(ORDER_ITEM_A, ORDER_ITEM_B, ORDER_ITEM_C) not in ((?, ?, ?), (?, ?, ?)) " +
                                        "for update"
                        );
                        it.variables("001", "001", 1, 1, 1, 1, 1, 3);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from ORDER_ITEM_PRODUCT_MAPPING " +
                                        "where (FK_ORDER_ITEM_A, FK_ORDER_ITEM_B, FK_ORDER_ITEM_C) in ((?, ?, ?))"
                        );
                        it.variables(1, 1, 2);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from ORDER_ITEM " +
                                        "where (ORDER_ITEM_A, ORDER_ITEM_B, ORDER_ITEM_C) in ((?, ?, ?))"
                        );
                        it.variables(1, 1, 2);
                    });
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
                                                .setOrder(order ->
                                                        order
                                                                .setId(OrderIdDraft.$.produce(id -> id.setX("001").setY("002")))
                                                                .setName("order-2")
                                                )
                                ),
                                true
                        ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ORDER_X, tb_1_.ORDER_Y from ORDER_ as tb_1_ " +
                                        "where (tb_1_.ORDER_X, tb_1_.ORDER_Y) = (?, ?) for update"
                        );
                        it.variables("001", "002");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update ORDER_ set NAME = ? " +
                                        "where (ORDER_X, ORDER_Y) = (?, ?)"
                        );
                        it.variables("order-2", "001", "002");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ORDER_ITEM_A, tb_1_.ORDER_ITEM_B, tb_1_.ORDER_ITEM_C " +
                                        "from ORDER_ITEM as tb_1_ " +
                                        "where (tb_1_.ORDER_ITEM_A, tb_1_.ORDER_ITEM_B, tb_1_.ORDER_ITEM_C) = (?, ?, ?) " +
                                        "for update"
                        );
                        it.variables(1, 1, 1);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update ORDER_ITEM set NAME = ?, FK_ORDER_X = ?, FK_ORDER_Y = ? " +
                                        "where (ORDER_ITEM_A, ORDER_ITEM_B, ORDER_ITEM_C) = (?, ?, ?)"
                        );
                        it.variables("order-item-1-1", "001", "002", 1, 1, 1);
                    });
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
                                ),
                                true
                        ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ORDER_ITEM_A, tb_1_.ORDER_ITEM_B, tb_1_.ORDER_ITEM_C " +
                                        "from ORDER_ITEM as tb_1_ " +
                                        "where (tb_1_.ORDER_ITEM_A, tb_1_.ORDER_ITEM_B, tb_1_.ORDER_ITEM_C) = (?, ?, ?) " +
                                        "for update"
                        );
                        it.variables(1, 1, 1);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update ORDER_ITEM set NAME = ? " +
                                        "where (ORDER_ITEM_A, ORDER_ITEM_B, ORDER_ITEM_C) = (?, ?, ?)"
                        );
                        it.variables("order-item-1-1", 1, 1, 1);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.PRODUCT_ALPHA, tb_1_.PRODUCT_BETA " +
                                        "from PRODUCT as tb_1_ " +
                                        "where (tb_1_.PRODUCT_ALPHA, tb_1_.PRODUCT_BETA) = (?, ?) " +
                                        "for update"
                        );
                        it.variables("00A", "00B");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update PRODUCT set NAME = ? " +
                                        "where (PRODUCT_ALPHA, PRODUCT_BETA) = (?, ?)"
                        );
                        it.variables("Boat", "00A", "00B");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.PRODUCT_ALPHA, tb_1_.PRODUCT_BETA " +
                                        "from PRODUCT as tb_1_ " +
                                        "where (tb_1_.PRODUCT_ALPHA, tb_1_.PRODUCT_BETA) = (?, ?) " +
                                        "for update"
                        );
                        it.variables("00A", "00C");
                    });
                    ctx.statement(it -> {
                        it.sql("insert into PRODUCT(PRODUCT_ALPHA, PRODUCT_BETA, NAME) values(?, ?, ?)");
                        it.variables("00A", "00C", "Bus");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select FK_PRODUCT_ALPHA, FK_PRODUCT_BETA " +
                                        "from ORDER_ITEM_PRODUCT_MAPPING " +
                                        "where (FK_ORDER_ITEM_A, FK_ORDER_ITEM_B, FK_ORDER_ITEM_C) = (?, ?, ?)"
                        );
                        it.variables(1, 1, 1);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from ORDER_ITEM_PRODUCT_MAPPING " +
                                        "where (" +
                                        "--->FK_ORDER_ITEM_A, " +
                                        "--->FK_ORDER_ITEM_B, " +
                                        "--->FK_ORDER_ITEM_C, " +
                                        "--->FK_PRODUCT_ALPHA, " +
                                        "--->FK_PRODUCT_BETA" +
                                        ") in (" +
                                        "--->(?, ?, ?, ?, ?), " +
                                        "--->(?, ?, ?, ?, ?)" +
                                        ")");
                        it.variables(
                                1, 1, 1, "00A", "00A",
                                1, 1, 1, "00B", "00A"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into ORDER_ITEM_PRODUCT_MAPPING(" +
                                        "--->FK_ORDER_ITEM_A, " +
                                        "--->FK_ORDER_ITEM_B, " +
                                        "--->FK_ORDER_ITEM_C, " +
                                        "--->FK_PRODUCT_ALPHA, " +
                                        "--->FK_PRODUCT_BETA" +
                                        ") values " +
                                        "--->(?, ?, ?, ?, ?), " +
                                        "--->(?, ?, ?, ?, ?)"
                        );
                        it.variables(
                                1, 1, 1, "00A", "00B",
                                1, 1, 1, "00A", "00C"
                        );
                    });
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
                                                .setId(id -> id.setAlpha("00A").setBeta("00A"))
                                                .setName("Car")
                                                .addIntoOrderItems(item ->
                                                        item.setId(id -> id.setA(1).setB(2).setC(1))
                                                )
                                ),
                                true
                        ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.PRODUCT_ALPHA, tb_1_.PRODUCT_BETA " +
                                        "from PRODUCT as tb_1_ " +
                                        "where (tb_1_.PRODUCT_ALPHA, tb_1_.PRODUCT_BETA) = (?, ?) " +
                                        "for update"
                        );
                        it.variables("00A", "00A");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update PRODUCT set NAME = ? " +
                                        "where (PRODUCT_ALPHA, PRODUCT_BETA) = (?, ?)"
                        );
                        it.variables("Car", "00A", "00A");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select FK_ORDER_ITEM_A, FK_ORDER_ITEM_B, FK_ORDER_ITEM_C " +
                                        "from ORDER_ITEM_PRODUCT_MAPPING " +
                                        "where (FK_PRODUCT_ALPHA, FK_PRODUCT_BETA) = (?, ?)"
                        );
                        it.variables("00A", "00A");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from ORDER_ITEM_PRODUCT_MAPPING " +
                                        "where (" +
                                        "--->FK_PRODUCT_ALPHA, " +
                                        "--->FK_PRODUCT_BETA, " +
                                        "--->FK_ORDER_ITEM_A, " +
                                        "--->FK_ORDER_ITEM_B, " +
                                        "--->FK_ORDER_ITEM_C" +
                                        ") in (" +
                                        "--->(?, ?, ?, ?, ?), " +
                                        "--->(?, ?, ?, ?, ?)" +
                                        ")"
                        );
                        it.variables(
                                "00A", "00A", 1, 1, 1,
                                "00A", "00A", 1, 1, 2
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into ORDER_ITEM_PRODUCT_MAPPING(" +
                                        "--->FK_PRODUCT_ALPHA, " +
                                        "--->FK_PRODUCT_BETA, " +
                                        "--->FK_ORDER_ITEM_A, " +
                                        "--->FK_ORDER_ITEM_B, " +
                                        "--->FK_ORDER_ITEM_C" +
                                        ") values (" +
                                        "--->?, ?, ?, ?, ?" +
                                        ")"
                        );
                        it.variables("00A", "00A", 1, 2, 1);
                    });
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
                        .configure(it ->
                                it.setDissociateAction(OrderItemProps.ORDER, DissociateAction.SET_NULL)
                        ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update ORDER_ITEM " +
                                        "set FK_ORDER_X = null, FK_ORDER_Y = null " +
                                        "where (FK_ORDER_X, FK_ORDER_Y) = (?, ?)"
                        );
                        it.variables("001", "001");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from ORDER_ " +
                                        "where (ORDER_X, ORDER_Y) in ((?, ?))"
                        );
                        it.variables("001", "001");
                    });
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
                        .configure(it ->
                                it.setDissociateAction(
                                        OrderItemProps.ORDER,
                                        DissociateAction.DELETE
                                )
                        ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select ORDER_ITEM_A, ORDER_ITEM_B, ORDER_ITEM_C " +
                                        "from ORDER_ITEM " +
                                        "where (FK_ORDER_X, FK_ORDER_Y) in ((?, ?))"
                        );
                        it.variables("001", "001");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from ORDER_ITEM_PRODUCT_MAPPING " +
                                        "where (" +
                                        "--->FK_ORDER_ITEM_A, FK_ORDER_ITEM_B, FK_ORDER_ITEM_C" +
                                        ") in (" +
                                        "--->(?, ?, ?), (?, ?, ?)" +
                                        ")"
                        );
                        it.variables(1, 1, 1, 1, 1, 2);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from ORDER_ITEM " +
                                        "where (" +
                                        "--->ORDER_ITEM_A, ORDER_ITEM_B, ORDER_ITEM_C" +
                                        ") in (" +
                                        "--->(?, ?, ?), (?, ?, ?)" +
                                        ")"
                        );
                        it.variables(1, 1, 1, 1, 1, 2);
                    });
                    ctx.statement(it -> {
                        it.sql("delete from ORDER_ where (ORDER_X, ORDER_Y) in ((?, ?))");
                        it.variables("001", "001");
                    });
                }
        );
    }
}
