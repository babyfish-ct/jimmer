package org.babyfish.jimmer.sql.middle;

import org.babyfish.jimmer.ImmutableObjects;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.model.middle.Customer;
import org.babyfish.jimmer.sql.model.middle.CustomerDraft;
import org.babyfish.jimmer.sql.model.middle.Shop;
import org.babyfish.jimmer.sql.model.middle.ShopDraft;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

public class SaveTest extends AbstractMutationTest {

    @Test
    public void testSaveShop() {
        Shop shop = ShopDraft.$.produce(draft -> {
            draft.setId(1L);
            draft.setVipCustomers(
                    Collections.singletonList(
                            ImmutableObjects.makeIdOnly(Customer.class, 2L)
                    )
            );
            draft.setOrdinaryCustomers(
                    Arrays.asList(
                            ImmutableObjects.makeIdOnly(Customer.class, 1L),
                            ImmutableObjects.makeIdOnly(Customer.class, 4L)
                    )
            );
        });
        executeAndExpectResult(
                getSqlClient().getEntities().saveCommand(shop),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("select tb_1_.ID, tb_1_.NAME from SHOP tb_1_ where tb_1_.ID = ?");
                        it.variables(1L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID " +
                                        "from CUSTOMER tb_1_ " +
                                        "inner join shop_customer_mapping tb_2_ on " +
                                        "--->tb_1_.ID = tb_2_.customer_id " +
                                        "and " +
                                        "--->tb_2_.deleted_millis = ? " +
                                        "and " +
                                        "--->tb_2_.type = ? " +
                                        "where tb_2_.shop_id = ?"
                        );
                        it.variables(0L, "VIP", 1L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from shop_customer_mapping " +
                                        "where " +
                                        "--->(shop_id, customer_id) = (?, ?) " +
                                        "and " +
                                        "--->deleted_millis = ? " +
                                        "and " +
                                        "--->type = ?"
                        );
                        it.variables(1L, 1L, 0L, "VIP");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into shop_customer_mapping(shop_id, customer_id, deleted_millis, type) " +
                                        "values(?, ?, ?, ?)"
                        );
                        it.variables(1L, 2L, 0L, "VIP");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID " +
                                        "from CUSTOMER tb_1_ " +
                                        "inner join shop_customer_mapping tb_2_ on " +
                                        "--->tb_1_.ID = tb_2_.customer_id " +
                                        "and " +
                                        "--->tb_2_.deleted_millis = ? " +
                                        "and " +
                                        "--->tb_2_.type = ? " +
                                        "where tb_2_.shop_id = ?"
                        );
                        it.variables(0L, "ORDINARY", 1L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from shop_customer_mapping " +
                                        "where " +
                                        "--->(shop_id, customer_id) in ((?, ?), (?, ?)) " +
                                        "and " +
                                        "--->deleted_millis = ? " +
                                        "and " +
                                        "--->type = ?"
                        );
                        it.variables(1L, 2L, 1L, 3L, 0L, "ORDINARY");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into shop_customer_mapping(shop_id, customer_id, deleted_millis, type) " +
                                        "values(?, ?, ?, ?), (?, ?, ?, ?)"
                        );
                        it.variables(1L, 1L, 0L, "ORDINARY", 1L, 4L, 0L, "ORDINARY");
                    });
                }
        );
    }
}
