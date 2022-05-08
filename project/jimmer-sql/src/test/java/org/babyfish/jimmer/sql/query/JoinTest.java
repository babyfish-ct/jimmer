package org.babyfish.jimmer.sql.query;

import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.BookTable;
import org.junit.jupiter.api.Test;

public class JoinTest extends AbstractQueryTest {

    @Test
    public void testSimple() {
        executeAndExpect(
                BookTable.createQuery(getSqlClient(), (query, book) ->
                    query.select(book)
                ),
                ctx -> {
                    ctx.sql(
                            "select " +
                                    "tb_1.ID, tb_1.NAME, tb_1.EDITION, tb_1.PRICE, tb_1.STORE_ID " +
                                    "from BOOK as tb_1"
                    );
                }
        );
    }
}
