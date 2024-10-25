package org.babyfish.jimmer.sql.mutation;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.TargetTransferMode;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.dialect.H2Dialect;
import org.babyfish.jimmer.sql.model.Immutables;
import org.babyfish.jimmer.sql.model.embedded.Order;
import org.junit.jupiter.api.Test;

public class NoTupleComparisonMutationTest extends AbstractMutationTest {

    private final JSqlClient sqlClient = getSqlClient(it -> {
        it.setDialect(
                new H2Dialect() {
                    @Override
                    public boolean isTupleComparisonSupported() {
                        return false;
                    }
                }
        );
    });

    @Test
    public void testDetach() {
        Order order = Immutables.createOrder(draft -> {
            draft.applyId(id -> id.setX("001").setY("001"));
            draft.addIntoOrderItems(item -> {
                item.applyId(id -> id.setA(10).setB(10).setC(10));
                item.setName("order-item-1-10");
                item.addIntoProducts(product -> {
                   product.applyId(id -> id.setAlpha("00B").setBeta("00A"));
                });
            });
        });
        executeAndExpectResult(
                sqlClient
                        .getEntities()
                        .saveCommand(order)
                        .setTargetTransferModeAll(TargetTransferMode.ALLOWED),
                ctx -> {
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
                        it.variables(10, 10, 10, "order-item-1-10", "001", "001");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from ORDER_ITEM_PRODUCT_MAPPING " +
                                        "where " +
                                        "--->FK_ORDER_ITEM_A = ? and " +
                                        "--->FK_ORDER_ITEM_B = ? and " +
                                        "--->FK_ORDER_ITEM_C = ? and (" +
                                        "--->--->FK_PRODUCT_ALPHA <> ? or FK_PRODUCT_BETA <> ?" +
                                        ")"
                        );
                        it.variables(10, 10, 10, "00B", "00A");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "merge into ORDER_ITEM_PRODUCT_MAPPING tb_1_ " +
                                        "using(values(?, ?, ?, ?, ?)) tb_2_(" +
                                        "--->FK_ORDER_ITEM_A, FK_ORDER_ITEM_B, FK_ORDER_ITEM_C, " +
                                        "--->FK_PRODUCT_ALPHA, FK_PRODUCT_BETA" +
                                        ") on " +
                                        "--->tb_1_.FK_ORDER_ITEM_A = tb_2_.FK_ORDER_ITEM_A and " +
                                        "--->tb_1_.FK_ORDER_ITEM_B = tb_2_.FK_ORDER_ITEM_B and " +
                                        "--->tb_1_.FK_ORDER_ITEM_C = tb_2_.FK_ORDER_ITEM_C and " +
                                        "--->tb_1_.FK_PRODUCT_ALPHA = tb_2_.FK_PRODUCT_ALPHA and " +
                                        "--->tb_1_.FK_PRODUCT_BETA = tb_2_.FK_PRODUCT_BETA " +
                                        "when not matched then " +
                                        "--->insert(" +
                                        "--->--->FK_ORDER_ITEM_A, FK_ORDER_ITEM_B, FK_ORDER_ITEM_C, " +
                                        "--->--->FK_PRODUCT_ALPHA, FK_PRODUCT_BETA" +
                                        "--->) values(" +
                                        "--->--->tb_2_.FK_ORDER_ITEM_A, tb_2_.FK_ORDER_ITEM_B, tb_2_.FK_ORDER_ITEM_C, " +
                                        "--->--->tb_2_.FK_PRODUCT_ALPHA, tb_2_.FK_PRODUCT_BETA" +
                                        "--->)"
                        );
                        it.variables(10, 10, 10, "00B", "00A");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from ORDER_ITEM_PRODUCT_MAPPING tb_1_ " +
                                        "where exists (" +
                                        "--->select * " +
                                        "--->from ORDER_ITEM tb_2_ " +
                                        "--->where " +
                                        "--->--->tb_1_.FK_ORDER_ITEM_A = tb_2_.ORDER_ITEM_A and " +
                                        "--->--->tb_1_.FK_ORDER_ITEM_B = tb_2_.ORDER_ITEM_B and " +
                                        "--->--->tb_1_.FK_ORDER_ITEM_C = tb_2_.ORDER_ITEM_C and " +
                                        "--->--->tb_2_.FK_ORDER_X = ? and " +
                                        "--->--->tb_2_.FK_ORDER_Y = ? and (" +
                                        "--->--->--->tb_2_.ORDER_ITEM_A <> ? or " +
                                        "--->--->--->tb_2_.ORDER_ITEM_B <> ? or " +
                                        "--->--->--->tb_2_.ORDER_ITEM_C <> ?" +
                                        "--->--->)" +
                                        ")"
                        );
                        it.variables("001", "001", 10, 10, 10);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from ORDER_ITEM " +
                                        "where " +
                                        "--->FK_ORDER_X = ? and FK_ORDER_Y = ? and (" +
                                        "--->--->ORDER_ITEM_A <> ? or " +
                                        "--->--->ORDER_ITEM_B <> ? or " +
                                        "--->--->ORDER_ITEM_C <> ?" +
                                        "--->)"
                        );
                        it.variables("001", "001", 10, 10, 10);
                    });
                    ctx.entity(it -> {});
                }
        );
    }
}
