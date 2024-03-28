package org.babyfish.jimmer.sql.fetcher;

import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.BookFetcher;
import org.babyfish.jimmer.sql.model.BookStoreFetcher;
import org.babyfish.jimmer.sql.model.BookStoreTable;
import org.babyfish.jimmer.sql.model.BookTable;
import org.junit.jupiter.api.Test;

public class FormulaTest extends AbstractQueryTest {

    @Test
    public void testAuthorCount() {
        BookTable table = BookTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.name().like("Learning GraphQL"))
                        .orderBy(table.name().asc(), table.edition().desc())
                        .select(
                                table.fetch(
                                        BookFetcher.$.authorCount()
                                )
                        ),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID " +
                                    "from BOOK tb_1_ " +
                                    "where tb_1_.NAME like ? " +
                                    "order by tb_1_.NAME asc, tb_1_.EDITION desc"
                    );
                    ctx.statement(1).sql(
                            "select tb_1_.BOOK_ID, tb_1_.AUTHOR_ID " +
                                    "from BOOK_AUTHOR_MAPPING tb_1_ " +
                                    "where tb_1_.BOOK_ID in (?, ?, ?)"
                    );
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":\"64873631-5d82-4bae-8eb8-72dd955bfc56\"," +
                                    "--->--->\"authorCount\":2" +
                                    "--->},{" +
                                    "--->--->\"id\":\"b649b11b-1161-4ad2-b261-af0112fdd7c8\"," +
                                    "--->--->\"authorCount\":2" +
                                    "--->},{" +
                                    "--->--->\"id\":\"e110c564-23cc-4811-9e81-d587a13db634\"," +
                                    "--->--->\"authorCount\":2" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testAuthorFullNames() {
        BookTable table = BookTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.name().like("Learning GraphQL"))
                        .orderBy(table.name().asc(), table.edition().desc())
                        .select(
                                table.fetch(
                                        BookFetcher.$.authorFullNames()
                                )
                        ),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID " +
                                    "from BOOK tb_1_ " +
                                    "where tb_1_.NAME like ? " +
                                    "order by tb_1_.NAME asc, tb_1_.EDITION desc"
                    );
                    ctx.statement(1).sql(
                            "select tb_2_.BOOK_ID, tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME " +
                                    "from AUTHOR tb_1_ " +
                                    "inner join BOOK_AUTHOR_MAPPING tb_2_ on tb_1_.ID = tb_2_.AUTHOR_ID " +
                                    "where tb_2_.BOOK_ID in (?, ?, ?)"
                    );
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":\"64873631-5d82-4bae-8eb8-72dd955bfc56\"," +
                                    "--->--->\"authorFullNames\":[\"Alex-Banks\",\"Eve-Procello\"]" +
                                    "--->},{" +
                                    "--->--->\"id\":\"b649b11b-1161-4ad2-b261-af0112fdd7c8\"," +
                                    "--->--->\"authorFullNames\":[\"Alex-Banks\",\"Eve-Procello\"]" +
                                    "--->},{" +
                                    "--->--->\"id\":\"e110c564-23cc-4811-9e81-d587a13db634\"," +
                                    "--->--->\"authorFullNames\":[\"Alex-Banks\",\"Eve-Procello\"]" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testBookStoreMaxPrice() {
        BookStoreTable table = BookStoreTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .orderBy(table.name().asc())
                        .select(
                                table.fetch(
                                        BookStoreFetcher.$.maxPrice()
                                )
                        ),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID " +
                                    "from BOOK_STORE tb_1_ " +
                                    "order by tb_1_.NAME asc"
                    );
                    ctx.statement(1).sql(
                            "select tb_1_.STORE_ID, tb_1_.ID, tb_1_.PRICE " +
                                    "from BOOK tb_1_ " +
                                    "where tb_1_.STORE_ID in (?, ?)"
                    );
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\",\"maxPrice\":81.00}," +
                                    "--->--->{\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\",\"maxPrice\":88.00" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }
}
