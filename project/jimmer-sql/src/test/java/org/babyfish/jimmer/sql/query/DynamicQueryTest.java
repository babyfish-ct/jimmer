package org.babyfish.jimmer.sql.query;

import org.babyfish.jimmer.sql.ast.query.Order;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.BookTable;
import org.babyfish.jimmer.sql.model.embedded.TransformTable;
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
                                    "from BOOK as tb_1_ " +
                                    "left join BOOK_STORE as tb_2_ on tb_1_.STORE_ID = tb_2_.ID " +
                                    "order by tb_2_.NAME asc nulls first, " +
                                    "tb_1_.NAME asc, " +
                                    "tb_1_.EDITION desc"
                    );
                }
        );
    }

    @Test
    public void testEmbeddedProp() {
        TransformTable table = TransformTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .orderBy(
                                Order.makeOrders(
                                        table,
                                        "source.leftTop.x asc, target.rightBottom.y desc"
                                )
                        )
                        .select(table),
                ctx -> {
                    ctx.sql(
                            "select " +
                                    "tb_1_.ID, " +
                                    "tb_1_.`LEFT`, tb_1_.TOP, tb_1_.`RIGHT`, tb_1_.BOTTOM, " +
                                    "tb_1_.TARGET_LEFT, tb_1_.TARGET_TOP, tb_1_.TARGET_RIGHT, tb_1_.TARGET_BOTTOM " +
                                    "from TRANSFORM as tb_1_ " +
                                    "order by tb_1_.`LEFT` asc, tb_1_.TARGET_BOTTOM desc"
                    );
                }
        );
    }
}
