package org.babyfish.jimmer.sql.middle;

import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.middle.CustomerFetcher;
import org.babyfish.jimmer.sql.model.middle.CustomerTable;
import org.babyfish.jimmer.sql.model.middle.ShopFetcher;
import org.babyfish.jimmer.sql.model.middle.ShopTable;
import org.babyfish.jimmer.sql.runtime.LogicalDeletedBehavior;
import org.junit.jupiter.api.Test;

public class FetcherTest extends AbstractQueryTest {

    @Test
    public void testQueryShopWithCustomerId() {
        ShopTable table = ShopTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .orderBy(table.name())
                        .select(
                                table.fetch(
                                        ShopFetcher.$
                                                .allScalarFields()
                                                .customers()
                                                .vipCustomers()
                                                .ordinaryCustomers()
                                )
                        ),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME from SHOP tb_1_ order by tb_1_.NAME asc"
                    );
                    ctx.statement(1).sql(
                            "select tb_1_.shop_id, tb_1_.customer_id " +
                                    "from shop_customer_mapping tb_1_ " +
                                    "inner join CUSTOMER tb_3_ on " +
                                    "--->tb_1_.customer_id = tb_3_.ID " +
                                    "and " +
                                    "--->tb_1_.deleted_millis = ? " +
                                    "where tb_1_.shop_id in (?, ?) " +
                                    "order by tb_3_.NAME asc"
                    );
                    ctx.statement(2).sql(
                            "select tb_1_.shop_id, tb_1_.customer_id " +
                                    "from shop_customer_mapping tb_1_ " +
                                    "inner join CUSTOMER tb_3_ on " +
                                    "--->tb_1_.customer_id = tb_3_.ID " +
                                    "and " +
                                    "--->tb_1_.deleted_millis = ? " +
                                    "and " +
                                    "--->tb_1_.type = ? " +
                                    "where tb_1_.shop_id in (?, ?) " +
                                    "order by tb_3_.NAME asc"
                    );
                    ctx.statement(3).sql(
                            "select tb_1_.shop_id, tb_1_.customer_id " +
                                    "from shop_customer_mapping tb_1_ " +
                                    "inner join CUSTOMER tb_3_ on " +
                                    "--->tb_1_.customer_id = tb_3_.ID " +
                                    "and " +
                                    "--->tb_1_.deleted_millis = ? " +
                                    "and " +
                                    "--->tb_1_.type = ? " +
                                    "where tb_1_.shop_id in (?, ?) " +
                                    "order by tb_3_.NAME asc"
                    );
                    ctx.rows(
                            "[" +
                                    "--->{\"id\":2," +
                                    "--->--->\"name\":\"Dunkin\"," +
                                    "--->--->\"customers\":[{\"id\":3},{\"id\":4},{\"id\":5}]," +
                                    "--->--->\"vipCustomers\":[{\"id\":3}]," +
                                    "--->--->\"ordinaryCustomers\":[{\"id\":4},{\"id\":5}]" +
                                    "--->},{" +
                                    "--->--->\"id\":1," +
                                    "--->--->\"name\":\"Starbucks\"," +
                                    "--->--->\"customers\":[{\"id\":1},{\"id\":3},{\"id\":2}]," +
                                    "--->--->\"vipCustomers\":[{\"id\":1}]," +
                                    "--->--->\"ordinaryCustomers\":[{\"id\":3},{\"id\":2}]" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testQueryShopWithCustomers() {
        ShopTable table = ShopTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .orderBy(table.name())
                        .select(
                                table.fetch(
                                        ShopFetcher.$
                                                .allScalarFields()
                                                .customers(
                                                        CustomerFetcher.$.allScalarFields()
                                                )
                                                .vipCustomers(
                                                        CustomerFetcher.$.allScalarFields()
                                                )
                                                .ordinaryCustomers(
                                                        CustomerFetcher.$.allScalarFields()
                                                )
                                )
                        ),
                ctx -> {
                    ctx.sql("select tb_1_.ID, tb_1_.NAME from SHOP tb_1_ order by tb_1_.NAME asc");
                    ctx.statement(1).sql(
                            "select tb_2_.shop_id, tb_1_.ID, tb_1_.NAME " +
                                    "from CUSTOMER tb_1_ " +
                                    "inner join shop_customer_mapping tb_2_ on " +
                                    "--->tb_1_.ID = tb_2_.customer_id " +
                                    "and " +
                                    "--->tb_2_.deleted_millis = ? " +
                                    "where tb_2_.shop_id in (?, ?) " +
                                    "order by tb_1_.NAME asc"
                    );
                    ctx.statement(2).sql(
                            "select tb_2_.shop_id, tb_1_.ID, tb_1_.NAME " +
                                    "from CUSTOMER tb_1_ " +
                                    "inner join shop_customer_mapping tb_2_ on " +
                                    "--->tb_1_.ID = tb_2_.customer_id " +
                                    "and " +
                                    "--->tb_2_.deleted_millis = ? " +
                                    "and " +
                                    "--->tb_2_.type = ? " +
                                    "where tb_2_.shop_id in (?, ?) " +
                                    "order by tb_1_.NAME asc"
                    );
                    ctx.statement(3).sql(
                            "select tb_2_.shop_id, tb_1_.ID, tb_1_.NAME " +
                                    "from CUSTOMER tb_1_ " +
                                    "inner join shop_customer_mapping tb_2_ on " +
                                    "--->tb_1_.ID = tb_2_.customer_id " +
                                    "and " +
                                    "--->tb_2_.deleted_millis = ? " +
                                    "and " +
                                    "--->tb_2_.type = ? " +
                                    "where tb_2_.shop_id in (?, ?) " +
                                    "order by tb_1_.NAME asc"
                    );
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":2," +
                                    "--->--->\"name\":\"Dunkin\"," +
                                    "--->--->\"customers\":[" +
                                    "--->--->--->{\"id\":3,\"name\":\"Jessica\"}," +
                                    "--->--->--->{\"id\":4,\"name\":\"Linda\"}," +
                                    "--->--->--->{\"id\":5,\"name\":\"Mary\"}" +
                                    "--->--->]," +
                                    "--->--->\"vipCustomers\":[" +
                                    "--->--->--->{\"id\":3,\"name\":\"Jessica\"}" +
                                    "--->--->]," +
                                    "--->--->\"ordinaryCustomers\":[" +
                                    "--->--->--->{\"id\":4,\"name\":\"Linda\"}," +
                                    "--->--->--->{\"id\":5,\"name\":\"Mary\"}" +
                                    "--->--->]" +
                                    "--->},{" +
                                    "--->--->\"id\":1," +
                                    "--->--->\"name\":\"Starbucks\"," +
                                    "--->--->\"customers\":[" +
                                    "--->--->--->{\"id\":1,\"name\":\"Alex\"}," +
                                    "--->--->--->{\"id\":3,\"name\":\"Jessica\"}," +
                                    "--->--->--->{\"id\":2,\"name\":\"Tim\"}" +
                                    "--->--->]," +
                                    "--->--->\"vipCustomers\":[" +
                                    "--->--->--->{\"id\":1,\"name\":\"Alex\"}" +
                                    "--->--->]," +
                                    "--->--->\"ordinaryCustomers\":[" +
                                    "--->--->--->{\"id\":3,\"name\":\"Jessica\"}," +
                                    "--->--->--->{\"id\":2,\"name\":\"Tim\"}" +
                                    "--->--->]" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void findCustomerWithShopIds() {
        CustomerTable table = CustomerTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .orderBy(table.name())
                        .select(
                                table.fetch(
                                        CustomerFetcher.$
                                                .allScalarFields()
                                                .shops()
                                                .vipShops()
                                                .ordinaryShops()
                                )
                        ),
                ctx -> {
                    ctx.sql("select tb_1_.ID, tb_1_.NAME from CUSTOMER tb_1_ order by tb_1_.NAME asc");
                    ctx.statement(1).sql(
                            "select tb_1_.customer_id, tb_1_.shop_id " +
                                    "from shop_customer_mapping tb_1_ " +
                                    "inner join SHOP tb_3_ on " +
                                    "--->tb_1_.shop_id = tb_3_.ID " +
                                    "and " +
                                    "--->tb_1_.deleted_millis = ? " +
                                    "where tb_1_.customer_id in (?, ?, ?, ?, ?, ?) " +
                                    "order by tb_3_.NAME asc"
                    );
                    ctx.statement(2).sql(
                            "select tb_1_.customer_id, tb_1_.shop_id " +
                                    "from shop_customer_mapping tb_1_ " +
                                    "inner join SHOP tb_3_ on " +
                                    "--->tb_1_.shop_id = tb_3_.ID " +
                                    "and " +
                                    "--->tb_1_.deleted_millis = ? " +
                                    "and " +
                                    "--->tb_1_.type = ? " +
                                    "where tb_1_.customer_id in (?, ?, ?, ?, ?, ?) " +
                                    "order by tb_3_.NAME asc"
                    );
                    ctx.statement(3).sql(
                            "select tb_1_.customer_id, tb_1_.shop_id " +
                                    "from shop_customer_mapping tb_1_ " +
                                    "inner join SHOP tb_3_ on " +
                                    "--->tb_1_.shop_id = tb_3_.ID " +
                                    "and " +
                                    "--->tb_1_.deleted_millis = ? " +
                                    "and " +
                                    "--->tb_1_.type = ? " +
                                    "where tb_1_.customer_id in (?, ?, ?, ?, ?, ?) " +
                                    "order by tb_3_.NAME asc"
                    );
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":1," +
                                    "--->--->\"name\":\"Alex\"," +
                                    "--->--->\"shops\":[{\"id\":1}]," +
                                    "--->--->\"vipShops\":[{\"id\":1}]," +
                                    "--->--->\"ordinaryShops\":[]" +
                                    "--->},{" +
                                    "--->--->\"id\":6," +
                                    "--->--->\"name\":\"Bob\"," +
                                    "--->--->\"shops\":[],\"vipShops\":[]," +
                                    "--->--->\"ordinaryShops\":[]" +
                                    "--->},{" +
                                    "--->--->\"id\":3," +
                                    "--->--->\"name\":\"Jessica\"," +
                                    "--->--->\"shops\":[{\"id\":2},{\"id\":1}]," +
                                    "--->--->\"vipShops\":[{\"id\":2}]," +
                                    "--->--->\"ordinaryShops\":[{\"id\":1}]" +
                                    "--->},{" +
                                    "--->--->\"id\":4," +
                                    "--->--->\"name\":\"Linda\"," +
                                    "--->--->\"shops\":[{\"id\":2}]," +
                                    "--->--->\"vipShops\":[]," +
                                    "--->--->\"ordinaryShops\":[{\"id\":2}]" +
                                    "--->},{" +
                                    "--->--->\"id\":5," +
                                    "--->--->\"name\":\"Mary\"," +
                                    "--->--->\"shops\":[{\"id\":2}]," +
                                    "--->--->\"vipShops\":[]," +
                                    "--->--->\"ordinaryShops\":[{\"id\":2}]" +
                                    "--->},{" +
                                    "--->--->\"id\":2," +
                                    "--->--->\"name\":\"Tim\"," +
                                    "--->--->\"shops\":[{\"id\":1}]," +
                                    "--->--->\"vipShops\":[]," +
                                    "--->--->\"ordinaryShops\":[{\"id\":1}]" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testQueryCustomerWithShops() {
        CustomerTable table = CustomerTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .orderBy(table.name())
                        .select(
                                table.fetch(
                                        CustomerFetcher.$
                                                .allScalarFields()
                                                .shops(
                                                        ShopFetcher.$.allScalarFields()
                                                )
                                                .vipShops(
                                                        ShopFetcher.$.allScalarFields()
                                                )
                                                .ordinaryShops(
                                                        ShopFetcher.$.allScalarFields()
                                                )
                                )
                        ),
                ctx -> {
                    ctx.sql("select tb_1_.ID, tb_1_.NAME from CUSTOMER tb_1_ order by tb_1_.NAME asc");
                    ctx.statement(1).sql(
                            "select tb_2_.customer_id, tb_1_.ID, tb_1_.NAME " +
                                    "from SHOP tb_1_ " +
                                    "inner join shop_customer_mapping tb_2_ on " +
                                    "--->tb_1_.ID = tb_2_.shop_id " +
                                    "and " +
                                    "--->tb_2_.deleted_millis = ? " +
                                    "where tb_2_.customer_id in (?, ?, ?, ?, ?, ?) " +
                                    "order by tb_1_.NAME asc"
                    );
                    ctx.statement(2).sql(
                            "select tb_2_.customer_id, tb_1_.ID, tb_1_.NAME " +
                                    "from SHOP tb_1_ " +
                                    "inner join shop_customer_mapping tb_2_ on " +
                                    "--->tb_1_.ID = tb_2_.shop_id " +
                                    "and " +
                                    "--->tb_2_.deleted_millis = ? " +
                                    "and " +
                                    "--->tb_2_.type = ? " +
                                    "where tb_2_.customer_id in (?, ?, ?, ?, ?, ?) " +
                                    "order by tb_1_.NAME asc"
                    );
                    ctx.statement(3).sql(
                            "select tb_2_.customer_id, tb_1_.ID, tb_1_.NAME " +
                                    "from SHOP tb_1_ " +
                                    "inner join shop_customer_mapping tb_2_ on " +
                                    "--->tb_1_.ID = tb_2_.shop_id " +
                                    "and " +
                                    "--->tb_2_.deleted_millis = ? " +
                                    "and " +
                                    "--->tb_2_.type = ? " +
                                    "where tb_2_.customer_id in (?, ?, ?, ?, ?, ?) " +
                                    "order by tb_1_.NAME asc"
                    );
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":1," +
                                    "--->--->\"name\":\"Alex\"," +
                                    "--->--->\"shops\":[{\"id\":1,\"name\":\"Starbucks\"}]," +
                                    "--->--->\"vipShops\":[{\"id\":1,\"name\":\"Starbucks\"}]," +
                                    "--->--->\"ordinaryShops\":[]" +
                                    "--->},{" +
                                    "--->--->\"id\":6," +
                                    "--->--->\"name\":\"Bob\"," +
                                    "--->--->\"shops\":[]," +
                                    "--->--->\"vipShops\":[]," +
                                    "--->--->\"ordinaryShops\":[]" +
                                    "--->},{" +
                                    "--->--->\"id\":3," +
                                    "--->--->\"name\":\"Jessica\"," +
                                    "--->--->\"shops\":[" +
                                    "--->--->--->{\"id\":2,\"name\":\"Dunkin\"}," +
                                    "--->--->--->{\"id\":1,\"name\":\"Starbucks\"}" +
                                    "--->--->]," +
                                    "--->--->\"vipShops\":[{\"id\":2,\"name\":\"Dunkin\"}]," +
                                    "--->--->\"ordinaryShops\":[{\"id\":1,\"name\":\"Starbucks\"}]" +
                                    "--->},{" +
                                    "--->--->\"id\":4," +
                                    "--->--->\"name\":\"Linda\"," +
                                    "--->--->\"shops\":[{\"id\":2,\"name\":\"Dunkin\"}]," +
                                    "--->--->\"vipShops\":[]," +
                                    "--->--->\"ordinaryShops\":[{\"id\":2,\"name\":\"Dunkin\"}]" +
                                    "--->},{" +
                                    "--->--->\"id\":5," +
                                    "--->--->\"name\":\"Mary\"," +
                                    "--->--->\"shops\":[{\"id\":2,\"name\":\"Dunkin\"}]," +
                                    "--->--->\"vipShops\":[]," +
                                    "--->--->\"ordinaryShops\":[{\"id\":2,\"name\":\"Dunkin\"}]" +
                                    "--->},{" +
                                    "--->--->\"id\":2," +
                                    "--->--->\"name\":\"Tim\"," +
                                    "--->--->\"shops\":[{\"id\":1,\"name\":\"Starbucks\"}]," +
                                    "--->--->\"vipShops\":[]," +
                                    "--->--->\"ordinaryShops\":[{\"id\":1,\"name\":\"Starbucks\"}]" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testQueryWithoutLogicalDeleted() {
        ShopTable table = ShopTable.$;
        executeAndExpect(
                getSqlClient(it -> it.setLogicalDeletedBehavior(LogicalDeletedBehavior.IGNORED))
                        .createQuery(table)
                        .orderBy(table.name())
                        .select(
                                table.fetch(
                                        ShopFetcher.$
                                                .allScalarFields()
                                                .customers(
                                                        CustomerFetcher.$.allScalarFields()
                                                )
                                                .vipCustomers(
                                                        CustomerFetcher.$.allScalarFields()
                                                )
                                                .ordinaryCustomers(
                                                        CustomerFetcher.$.allScalarFields()
                                                )
                                )
                        ),
                ctx -> {
                    ctx.sql("select tb_1_.ID, tb_1_.NAME from SHOP tb_1_ order by tb_1_.NAME asc");
                    ctx.statement(1).sql(
                            "select tb_2_.shop_id, tb_1_.ID, tb_1_.NAME " +
                                    "from CUSTOMER tb_1_ " +
                                    "inner join shop_customer_mapping tb_2_ on tb_1_.ID = tb_2_.customer_id " +
                                    "where tb_2_.shop_id in (?, ?) " +
                                    "order by tb_1_.NAME asc"
                    );
                    ctx.statement(2).sql(
                            "select tb_2_.shop_id, tb_1_.ID, tb_1_.NAME " +
                                    "from CUSTOMER tb_1_ " +
                                    "inner join shop_customer_mapping tb_2_ on " +
                                    "--->tb_1_.ID = tb_2_.customer_id " +
                                    "and " +
                                    "--->tb_2_.type = ? " +
                                    "where tb_2_.shop_id in (?, ?) " +
                                    "order by tb_1_.NAME asc"
                    );
                    ctx.statement(3).sql(
                            "select tb_2_.shop_id, tb_1_.ID, tb_1_.NAME " +
                                    "from CUSTOMER tb_1_ " +
                                    "inner join shop_customer_mapping tb_2_ on " +
                                    "--->tb_1_.ID = tb_2_.customer_id " +
                                    "and " +
                                    "--->tb_2_.type = ? " +
                                    "where tb_2_.shop_id in (?, ?) " +
                                    "order by tb_1_.NAME asc"
                    );
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":2," +
                                    "--->--->\"name\":\"Dunkin\"," +
                                    "--->--->\"customers\":[" +
                                    "--->--->--->{\"id\":6,\"name\":\"Bob\"}," +
                                    "--->--->--->{\"id\":3,\"name\":\"Jessica\"}," +
                                    "--->--->--->{\"id\":4,\"name\":\"Linda\"}," +
                                    "--->--->--->{\"id\":5,\"name\":\"Mary\"}" +
                                    "--->--->]," +
                                    "--->--->\"vipCustomers\":[" +
                                    "--->--->--->{\"id\":3,\"name\":\"Jessica\"}" +
                                    "--->--->]," +
                                    "--->--->\"ordinaryCustomers\":[" +
                                    "--->--->--->{\"id\":6,\"name\":\"Bob\"}," +
                                    "--->--->--->{\"id\":4,\"name\":\"Linda\"}," +
                                    "--->--->--->{\"id\":5,\"name\":\"Mary\"}" +
                                    "--->--->]" +
                                    "--->},{" +
                                    "--->--->\"id\":1," +
                                    "--->--->\"name\":\"Starbucks\"," +
                                    "--->--->\"customers\":[" +
                                    "--->--->--->{\"id\":1,\"name\":\"Alex\"}," +
                                    "--->--->--->{\"id\":3,\"name\":\"Jessica\"}," +
                                    "--->--->--->{\"id\":4,\"name\":\"Linda\"}," +
                                    "--->--->--->{\"id\":2,\"name\":\"Tim\"}" +
                                    "--->--->]," +
                                    "--->--->\"vipCustomers\":[" +
                                    "--->--->--->{\"id\":1,\"name\":\"Alex\"}" +
                                    "--->--->]," +
                                    "--->--->\"ordinaryCustomers\":[" +
                                    "--->--->--->{\"id\":3,\"name\":\"Jessica\"}," +
                                    "--->--->--->{\"id\":4,\"name\":\"Linda\"}," +
                                    "--->--->--->{\"id\":2,\"name\":\"Tim\"}" +
                                    "--->--->]" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testQueryByInverseLogicalDeleted() {
        ShopTable table = ShopTable.$;
        executeAndExpect(
                getSqlClient(it -> it.setLogicalDeletedBehavior(LogicalDeletedBehavior.REVERSED))
                        .createQuery(table)
                        .orderBy(table.name())
                        .select(
                                table.fetch(
                                        ShopFetcher.$
                                                .allScalarFields()
                                                .customers(
                                                        CustomerFetcher.$.allScalarFields()
                                                )
                                                .vipCustomers(
                                                        CustomerFetcher.$.allScalarFields()
                                                )
                                                .ordinaryCustomers(
                                                        CustomerFetcher.$.allScalarFields()
                                                )
                                )
                        ),
                ctx -> {
                    ctx.sql("select tb_1_.ID, tb_1_.NAME from SHOP tb_1_ order by tb_1_.NAME asc");
                    ctx.statement(1).sql(
                            "select tb_2_.shop_id, tb_1_.ID, tb_1_.NAME " +
                                    "from CUSTOMER tb_1_ " +
                                    "inner join shop_customer_mapping tb_2_ on " +
                                    "--->tb_1_.ID = tb_2_.customer_id " +
                                    "and " +
                                    "--->tb_2_.deleted_millis <> ? " +
                                    "where tb_2_.shop_id in (?, ?) " +
                                    "order by tb_1_.NAME asc"
                    );
                    ctx.statement(2).sql(
                            "select tb_2_.shop_id, tb_1_.ID, tb_1_.NAME " +
                                    "from CUSTOMER tb_1_ " +
                                    "inner join shop_customer_mapping tb_2_ on " +
                                    "--->tb_1_.ID = tb_2_.customer_id " +
                                    "and " +
                                    "--->tb_2_.deleted_millis <> ? " +
                                    "and " +
                                    "--->tb_2_.type = ? " +
                                    "where tb_2_.shop_id in (?, ?) " +
                                    "order by tb_1_.NAME asc"
                    );
                    ctx.statement(3).sql(
                            "select tb_2_.shop_id, tb_1_.ID, tb_1_.NAME " +
                                    "from CUSTOMER tb_1_ " +
                                    "inner join shop_customer_mapping tb_2_ on " +
                                    "--->tb_1_.ID = tb_2_.customer_id " +
                                    "and " +
                                    "--->tb_2_.deleted_millis <> ? " +
                                    "and " +
                                    "--->tb_2_.type = ? " +
                                    "where tb_2_.shop_id in (?, ?) " +
                                    "order by tb_1_.NAME asc"
                    );
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":2,\"name\":\"Dunkin\"," +
                                    "--->--->\"customers\":[{\"id\":6,\"name\":\"Bob\"}]," +
                                    "--->--->\"vipCustomers\":[]," +
                                    "--->--->\"ordinaryCustomers\":[{\"id\":6,\"name\":\"Bob\"}]" +
                                    "--->},{" +
                                    "--->--->\"id\":1," +
                                    "--->--->\"name\":\"Starbucks\"," +
                                    "--->--->\"customers\":[{\"id\":4,\"name\":\"Linda\"}]," +
                                    "--->--->\"vipCustomers\":[]," +
                                    "--->--->\"ordinaryCustomers\":[{\"id\":4,\"name\":\"Linda\"}]" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }
}
