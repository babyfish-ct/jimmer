package org.babyfish.jimmer.sql.middle;

import org.babyfish.jimmer.sql.ast.table.AssociationTable;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.Author;
import org.babyfish.jimmer.sql.model.AuthorTable;
import org.babyfish.jimmer.sql.model.middle.*;
import org.junit.jupiter.api.Test;

public class JoinTest extends AbstractQueryTest {

    @Test
    public void testJoinFromShopToCustomer() {
        ShopTable table = ShopTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(
                                table.asTableEx().customers().name().eq("Jessica")
                        )
                        .orderBy(table.id())
                        .select(table)
                        .distinct(),
                ctx -> {
                    ctx.sql(
                            "select distinct tb_1_.ID, tb_1_.NAME " +
                                    "from SHOP tb_1_ " +
                                    "inner join shop_customer_mapping tb_2_ on " +
                                    "--->tb_1_.ID = tb_2_.shop_id " +
                                    "and " +
                                    "--->tb_2_.deleted_millis = ? " +
                                    "inner join CUSTOMER tb_3_ on tb_2_.customer_id = tb_3_.ID " +
                                    "where tb_3_.NAME = ? " +
                                    "order by tb_1_.ID asc"
                    );
                    ctx.rows(
                            "[{\"id\":1,\"name\":\"Starbucks\"},{\"id\":2,\"name\":\"Dunkin\"}]"
                    );
                }
        );
    }

    @Test
    public void testJoinFromShopToVipCustomer() {
        ShopTable table = ShopTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(
                                table.asTableEx().vipCustomers().name().eq("Jessica")
                        )
                        .orderBy(table.id())
                        .select(table)
                        .distinct(),
                ctx -> {
                    ctx.sql(
                            "select distinct tb_1_.ID, tb_1_.NAME " +
                                    "from SHOP tb_1_ " +
                                    "inner join shop_customer_mapping tb_2_ on " +
                                    "--->tb_1_.ID = tb_2_.shop_id " +
                                    "and " +
                                    "--->tb_2_.deleted_millis = ? " +
                                    "and " +
                                    "--->tb_2_.type = ? " +
                                    "inner join CUSTOMER tb_3_ on tb_2_.customer_id = tb_3_.ID " +
                                    "where tb_3_.NAME = ? " +
                                    "order by tb_1_.ID asc"
                    );
                    ctx.rows(
                            "[{\"id\":2,\"name\":\"Dunkin\"}]"
                    );
                }
        );
    }

    @Test
    public void testJoinFromShopToOrdinaryCustomer() {
        ShopTable table = ShopTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(
                                table.asTableEx().ordinaryCustomers().name().eq("Jessica")
                        )
                        .orderBy(table.id())
                        .select(table)
                        .distinct(),
                ctx -> {
                    ctx.sql(
                            "select distinct tb_1_.ID, tb_1_.NAME " +
                                    "from SHOP tb_1_ " +
                                    "inner join shop_customer_mapping tb_2_ on " +
                                    "--->tb_1_.ID = tb_2_.shop_id " +
                                    "and " +
                                    "--->tb_2_.deleted_millis = ? " +
                                    "and " +
                                    "--->tb_2_.type = ? " +
                                    "inner join CUSTOMER tb_3_ on tb_2_.customer_id = tb_3_.ID " +
                                    "where tb_3_.NAME = ? " +
                                    "order by tb_1_.ID asc"
                    );
                    ctx.rows(
                            "[{\"id\":1,\"name\":\"Starbucks\"}]"
                    );
                }
        );
    }

    @Test
    public void testHalfJoinFromShopToCustomer() {
        ShopTable table = ShopTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(
                                table.asTableEx().customers().id().eq(3L)
                        )
                        .orderBy(table.id())
                        .select(table)
                        .distinct(),
                ctx -> {
                    ctx.sql(
                            "select distinct tb_1_.ID, tb_1_.NAME " +
                                    "from SHOP tb_1_ " +
                                    "inner join shop_customer_mapping tb_2_ on " +
                                    "--->tb_1_.ID = tb_2_.shop_id " +
                                    "and " +
                                    "--->tb_2_.deleted_millis = ? " +
                                    "where tb_2_.customer_id = ? " +
                                    "order by tb_1_.ID asc"
                    );
                    ctx.rows(
                            "[{\"id\":1,\"name\":\"Starbucks\"},{\"id\":2,\"name\":\"Dunkin\"}]"
                    );
                }
        );
    }

    @Test
    public void testHalfJoinFromShopToVipCustomer() {
        ShopTable table = ShopTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(
                                table.asTableEx().vipCustomers().id().eq(3L)
                        )
                        .orderBy(table.id())
                        .select(table)
                        .distinct(),
                ctx -> {
                    ctx.sql(
                            "select distinct tb_1_.ID, tb_1_.NAME " +
                                    "from SHOP tb_1_ " +
                                    "inner join shop_customer_mapping tb_2_ on " +
                                    "--->tb_1_.ID = tb_2_.shop_id " +
                                    "and " +
                                    "--->tb_2_.deleted_millis = ? " +
                                    "and " +
                                    "--->tb_2_.type = ? " +
                                    "where tb_2_.customer_id = ? " +
                                    "order by tb_1_.ID asc"
                    );
                    ctx.rows(
                            "[{\"id\":2,\"name\":\"Dunkin\"}]"
                    );
                }
        );
    }

    @Test
    public void testHalfJoinFromShopToOrdinaryCustomer() {
        ShopTable table = ShopTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(
                                table.asTableEx().ordinaryCustomers().id().eq(3L)
                        )
                        .orderBy(table.id())
                        .select(table)
                        .distinct(),
                ctx -> {
                    ctx.sql(
                            "select distinct tb_1_.ID, tb_1_.NAME " +
                                    "from SHOP tb_1_ " +
                                    "inner join shop_customer_mapping tb_2_ on " +
                                    "--->tb_1_.ID = tb_2_.shop_id " +
                                    "and " +
                                    "--->tb_2_.deleted_millis = ? " +
                                    "and " +
                                    "--->tb_2_.type = ? " +
                                    "where tb_2_.customer_id = ? " +
                                    "order by tb_1_.ID asc"
                    );
                    ctx.rows(
                            "[{\"id\":1,\"name\":\"Starbucks\"}]"
                    );
                }
        );
    }

    @Test
    public void testJoinFromCustomerToShop() {
        CustomerTable table = CustomerTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(
                                table.asTableEx().shops().name().eq("Starbucks")
                        )
                        .orderBy(table.id())
                        .select(table)
                        .distinct(),
                ctx -> {
                    ctx.sql(
                            "select distinct tb_1_.ID, tb_1_.NAME " +
                                    "from CUSTOMER tb_1_ " +
                                    "inner join shop_customer_mapping tb_2_ on " +
                                    "--->tb_1_.ID = tb_2_.customer_id " +
                                    "and " +
                                    "--->tb_2_.deleted_millis = ? " +
                                    "inner join SHOP tb_3_ on tb_2_.shop_id = tb_3_.ID " +
                                    "where tb_3_.NAME = ? " +
                                    "order by tb_1_.ID asc"
                    );
                    ctx.rows(
                            "[{\"id\":1,\"name\":\"Alex\"},{\"id\":2,\"name\":\"Tim\"},{\"id\":3,\"name\":\"Jessica\"}]"
                    );
                }
        );
    }

    @Test
    public void testJoinFromCustomerToVipShop() {
        CustomerTable table = CustomerTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(
                                table.asTableEx().vipShops().name().eq("Starbucks")
                        )
                        .orderBy(table.id())
                        .select(table)
                        .distinct(),
                ctx -> {
                    ctx.sql(
                            "select distinct tb_1_.ID, tb_1_.NAME " +
                                    "from CUSTOMER tb_1_ " +
                                    "inner join shop_customer_mapping tb_2_ on " +
                                    "--->tb_1_.ID = tb_2_.customer_id " +
                                    "and " +
                                    "--->tb_2_.deleted_millis = ? " +
                                    "and " +
                                    "--->tb_2_.type = ? " +
                                    "inner join SHOP tb_3_ on tb_2_.shop_id = tb_3_.ID " +
                                    "where tb_3_.NAME = ? " +
                                    "order by tb_1_.ID asc"
                    );
                    ctx.rows(
                            "[{\"id\":1,\"name\":\"Alex\"}]"
                    );
                }
        );
    }

    @Test
    public void testJoinFromCustomerToOrdinaryShop() {
        CustomerTable table = CustomerTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(
                                table.asTableEx().ordinaryShops().name().eq("Starbucks")
                        )
                        .orderBy(table.id())
                        .select(table)
                        .distinct(),
                ctx -> {
                    ctx.sql(
                            "select distinct tb_1_.ID, tb_1_.NAME " +
                                    "from CUSTOMER tb_1_ " +
                                    "inner join shop_customer_mapping tb_2_ on " +
                                    "--->tb_1_.ID = tb_2_.customer_id " +
                                    "and " +
                                    "--->tb_2_.deleted_millis = ? " +
                                    "and " +
                                    "--->tb_2_.type = ? " +
                                    "inner join SHOP tb_3_ on tb_2_.shop_id = tb_3_.ID " +
                                    "where tb_3_.NAME = ? " +
                                    "order by tb_1_.ID asc"
                    );
                    ctx.rows(
                            "[{\"id\":2,\"name\":\"Tim\"},{\"id\":3,\"name\":\"Jessica\"}]"
                    );
                }
        );
    }

    @Test
    public void testHalfJoinFromCustomerToShop() {
        CustomerTable table = CustomerTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(
                                table.asTableEx().shops().id().eq(1L)
                        )
                        .orderBy(table.id())
                        .select(table)
                        .distinct(),
                ctx -> {
                    ctx.sql(
                            "select distinct tb_1_.ID, tb_1_.NAME " +
                                    "from CUSTOMER tb_1_ " +
                                    "inner join shop_customer_mapping tb_2_ on " +
                                    "--->tb_1_.ID = tb_2_.customer_id " +
                                    "and " +
                                    "--->tb_2_.deleted_millis = ? " +
                                    "where tb_2_.shop_id = ? " +
                                    "order by tb_1_.ID asc"
                    );
                    ctx.rows(
                            "[{\"id\":1,\"name\":\"Alex\"},{\"id\":2,\"name\":\"Tim\"},{\"id\":3,\"name\":\"Jessica\"}]"
                    );
                }
        );
    }

    @Test
    public void testHalfJoinFromCustomerToVipShop() {
        CustomerTable table = CustomerTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(
                                table.asTableEx().vipShops().id().eq(1L)
                        )
                        .orderBy(table.id())
                        .select(table)
                        .distinct(),
                ctx -> {
                    ctx.sql(
                            "select distinct tb_1_.ID, tb_1_.NAME " +
                                    "from CUSTOMER tb_1_ " +
                                    "inner join shop_customer_mapping tb_2_ on " +
                                    "--->tb_1_.ID = tb_2_.customer_id " +
                                    "and " +
                                    "--->tb_2_.deleted_millis = ? " +
                                    "and " +
                                    "--->tb_2_.type = ? " +
                                    "where tb_2_.shop_id = ? " +
                                    "order by tb_1_.ID asc"
                    );
                    ctx.rows(
                            "[{\"id\":1,\"name\":\"Alex\"}]"
                    );
                }
        );
    }

    @Test
    public void testHalfJoinFromCustomerToOrdinaryShop() {
        CustomerTable table = CustomerTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(
                                table.asTableEx().ordinaryShops().id().eq(1L)
                        )
                        .orderBy(table.id())
                        .select(table)
                        .distinct(),
                ctx -> {
                    ctx.sql(
                            "select distinct tb_1_.ID, tb_1_.NAME " +
                                    "from CUSTOMER tb_1_ " +
                                    "inner join shop_customer_mapping tb_2_ on " +
                                    "--->tb_1_.ID = tb_2_.customer_id " +
                                    "and " +
                                    "--->tb_2_.deleted_millis = ? " +
                                    "and " +
                                    "--->tb_2_.type = ? " +
                                    "where tb_2_.shop_id = ? " +
                                    "order by tb_1_.ID asc"
                    );
                    ctx.rows(
                            "[{\"id\":2,\"name\":\"Tim\"},{\"id\":3,\"name\":\"Jessica\"}]"
                    );
                }
        );
    }

    @Test
    public void testInverseJoinFromShopToVipCustomer() {
        ShopTable table = ShopTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(
                                table.asTableEx().<CustomerTableEx>inverseJoin(CustomerProps.VIP_SHOPS).name().eq("Jessica")
                        )
                        .orderBy(table.name().asc())
                        .select(table),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME from SHOP tb_1_ " +
                                    "inner join shop_customer_mapping tb_2_ on " +
                                    "--->tb_1_.ID = tb_2_.shop_id " +
                                    "and " +
                                    "--->tb_2_.deleted_millis = ? " +
                                    "and " +
                                    "--->tb_2_.type = ? " +
                                    "inner join CUSTOMER tb_3_ on tb_2_.customer_id = tb_3_.ID " +
                                    "where tb_3_.NAME = ? " +
                                    "order by tb_1_.NAME asc"
                    );
                    ctx.rows(
                            "[{\"id\":2,\"name\":\"Dunkin\"}]"
                    );
                }
        );
    }

    @Test
    public void testInverseJoinFromCustomerToVipShop() {
        CustomerTable table = CustomerTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(
                                table.asTableEx().<ShopTableEx>inverseJoin(ShopProps.VIP_CUSTOMERS).name().eq("Starbucks")
                        )
                        .orderBy(table.name())
                        .select(table),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME " +
                                    "from CUSTOMER tb_1_ " +
                                    "inner join shop_customer_mapping tb_2_ on " +
                                    "--->tb_1_.ID = tb_2_.customer_id " +
                                    "and " +
                                    "--->tb_2_.deleted_millis = ? " +
                                    "and " +
                                    "--->tb_2_.type = ? " +
                                    "inner join SHOP tb_3_ on tb_2_.shop_id = tb_3_.ID " +
                                    "where tb_3_.NAME = ? " +
                                    "order by tb_1_.NAME asc"
                    );
                    ctx.rows(
                            "[{\"id\":1,\"name\":\"Alex\"}]"
                    );
                }
        );
    }

    @Test
    public void testJoinFromMiddleTable() {
        AssociationTable<Shop, ShopTableEx, Customer, CustomerTableEx> table =
                AssociationTable.of(ShopTableEx.class, ShopTableEx::vipCustomers);
        executeAndExpect(
                getSqlClient()
                        .createAssociationQuery(table)
                        .where(table.target().name().eq("Jessica"))
                        .select(table.sourceId(), table.targetId()),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.shop_id, tb_1_.customer_id " +
                                    "from shop_customer_mapping tb_1_ " +
                                    "inner join CUSTOMER tb_3_ on " +
                                    "--->tb_1_.customer_id = tb_3_.ID " +
                                    "and " +
                                    "--->tb_1_.deleted_millis = ? " +
                                    "and " +
                                    "--->tb_1_.type = ? " +
                                    "where tb_3_.NAME = ?"
                    );
                    ctx.rows("[{\"_1\":2,\"_2\":3}]");
                }
        );
    }
}
