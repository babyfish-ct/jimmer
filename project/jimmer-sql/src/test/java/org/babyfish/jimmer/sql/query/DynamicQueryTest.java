package org.babyfish.jimmer.sql.query;

import org.babyfish.jimmer.sql.ast.query.Order;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.BookTable;
import org.junit.jupiter.api.Test;

public class DynamicQueryTest extends AbstractQueryTest {

    @Test
    public void testReferenceProp() {
        BookTable table = BookTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .orderBy(
                                Order.makeOrders(
                                        table,
                                        "store.name asc nulls first;" +
                                                "name," +
                                                "edition desc"
                                )
                        )
                        .select(table),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                    "from BOOK tb_1_ " +
                                    "left join BOOK_STORE tb_2_ on tb_1_.STORE_ID = tb_2_.ID " +
                                    "order by tb_2_.NAME asc nulls first, " +
                                    "tb_1_.NAME asc, " +
                                    "tb_1_.EDITION desc"
                    );
                }
        );
    }
}
