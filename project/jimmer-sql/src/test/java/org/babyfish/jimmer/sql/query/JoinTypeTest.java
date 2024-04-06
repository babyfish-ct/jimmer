package org.babyfish.jimmer.sql.query;

import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.query.ConfigurableRootQuery;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.Book;
import org.babyfish.jimmer.sql.model.BookTable;
import org.junit.jupiter.api.Test;

public class JoinTypeTest extends AbstractQueryTest {

    @Test
    public void testLeftJoin() {
        executeAndExpect(
                createQuery(null),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                    "from BOOK tb_1_ " +
                                    "left join BOOK_STORE tb_2_ on tb_1_.STORE_ID = tb_2_.ID " +
                                    "order by tb_2_.NAME asc"
                    );
                }
        );
    }

    @Test
    public void testInnerJoin() {
        executeAndExpect(
                createQuery("MANNING"),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                    "from BOOK tb_1_ " +
                                    "inner join BOOK_STORE tb_2_ on tb_1_.STORE_ID = tb_2_.ID " +
                                    "where tb_2_.NAME ilike ? " +
                                    "order by tb_2_.NAME asc"
                    );
                }
        );
    }

    private ConfigurableRootQuery<BookTable, Book> createQuery(String storeName) {
        BookTable table = BookTable.$;
        return getSqlClient()
                .createQuery(table)
                .where(table.store().name().ilikeIf(storeName))
                .orderBy(table.store(JoinType.LEFT).name().asc())
                .select(table);
    }
}
