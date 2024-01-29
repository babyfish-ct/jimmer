package org.babyfish.jimmer.sql.middle;

import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.middle.CustomerFetcher;
import org.babyfish.jimmer.sql.model.middle.ShopFetcher;
import org.babyfish.jimmer.sql.model.middle.ShopTable;
import org.junit.jupiter.api.Test;

public class FetcherTest extends AbstractQueryTest {

    @Test
    public void testQueryShopWithCustomer() {
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
                                                        //CustomerFetcher.$.allScalarFields()
                                                )
                                                .vipCustomers(
                                                        //CustomerFetcher.$.allScalarFields()
                                                )
                                                .ordinaryCustomers(
                                                        //CustomerFetcher.$.allScalarFields()
                                                )
                                )
                        ),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME from SHOP tb_1_ order by tb_1_.NAME asc"
                    );
//                    ctx.statement(1).sql(
//                            ""
//                    );
                    ctx.rows(
                            ""
                    );
                }
        );
    }
}
