package org.babyfish.jimmer.sql.fetcher;

import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.*;
import org.junit.jupiter.api.Test;

public class MultiLevelFetcherQueryTest extends AbstractQueryTest {

    @Test
    public void testFromBookStore() {
        executeAndExpect(
                getSqlClient().createQuery(BookStoreTable.class, (q, store) -> {
                    return q.select(
                            store.fetch(
                                    BookStoreFetcher.$
                                            .name()
                                            .books(
                                                    BookFetcher.$
                                                            .name()
                                                            .authors(
                                                                    AuthorFetcher.$
                                                                            .firstName()
                                                                            .lastName()
                                                                            .books(
                                                                                    BookFetcher.$
                                                                                            .name()
                                                                                            .store(
                                                                                                    BookStoreFetcher.$
                                                                                                            .name()
                                                                                            ),
                                                                                    it -> it.batch(3)
                                                                            ),
                                                                    it -> it.batch(5)
                                                            )
                                            )
                            )
                    );
                }),
                ctx -> {
                    ctx.sql("select tb_1_.ID, tb_1_.NAME from BOOK_STORE as tb_1_");
                    ctx.statement(1).sql(
                            "select tb_1_.STORE_ID, tb_1_.ID, tb_1_.NAME " +
                                    "from BOOK as tb_1_ " +
                                    "where tb_1_.STORE_ID in (?, ?)"
                    );
                    ctx.statement(2).sql(
                            "select " +
                                    "tb_1_.BOOK_ID, " +
                                    "tb_3_.ID, tb_3_.FIRST_NAME, tb_3_.LAST_NAME " +
                                    "from BOOK_AUTHOR_MAPPING as tb_1_ " +
                                    "inner join AUTHOR as tb_3_ on tb_1_.AUTHOR_ID = tb_3_.ID " +
                                    "where tb_1_.BOOK_ID in (?, ?, ?, ?, ?)"
                    );
                    ctx.statement(3).sql(
                            "select " +
                                    "tb_1_.BOOK_ID, " +
                                    "tb_3_.ID, tb_3_.FIRST_NAME, tb_3_.LAST_NAME " +
                                    "from BOOK_AUTHOR_MAPPING as tb_1_ " +
                                    "inner join AUTHOR as tb_3_ on tb_1_.AUTHOR_ID = tb_3_.ID " +
                                    "where tb_1_.BOOK_ID in (?, ?, ?, ?, ?)"
                    );
                    ctx.statement(4).sql(
                            "select " +
                                    "tb_1_.BOOK_ID, " +
                                    "tb_3_.ID, tb_3_.FIRST_NAME, tb_3_.LAST_NAME " +
                                    "from BOOK_AUTHOR_MAPPING as tb_1_ " +
                                    "inner join AUTHOR as tb_3_ on tb_1_.AUTHOR_ID = tb_3_.ID " +
                                    "where tb_1_.BOOK_ID in (?, ?)"
                    );
                    ctx.statement(5).sql(
                            "select " +
                                    "tb_1_.AUTHOR_ID, " +
                                    "tb_3_.ID, tb_3_.NAME, tb_3_.STORE_ID " +
                                    "from BOOK_AUTHOR_MAPPING as tb_1_ " +
                                    "inner join BOOK as tb_3_ on tb_1_.BOOK_ID = tb_3_.ID " +
                                    "where tb_1_.AUTHOR_ID in (?, ?, ?)"
                    );
                    ctx.statement(6).sql(
                            "select " +
                                    "tb_1_.AUTHOR_ID, " +
                                    "tb_3_.ID, tb_3_.NAME, tb_3_.STORE_ID " +
                                    "from BOOK_AUTHOR_MAPPING as tb_1_ " +
                                    "inner join BOOK as tb_3_ on tb_1_.BOOK_ID = tb_3_.ID " +
                                    "where tb_1_.AUTHOR_ID in (?, ?)"
                    );
                    ctx.statement(7).sql(
                            "select tb_1_.ID, tb_1_.NAME from BOOK_STORE as tb_1_ where tb_1_.ID in (?, ?)"
                    );
                    ctx.rows(System.out::println);
                }
        );
    }

    @Test
    public void testFromAuthor() {
        executeAndExpect(
                getSqlClient().createQuery(AuthorTable.class, (q, author) -> {
                    return q.select(
                            author.fetch(
                                    AuthorFetcher.$
                                            .firstName()
                                            .firstName()
                                            .books(
                                                    BookFetcher.$
                                                            .name()
                                                            .store(
                                                                    BookStoreFetcher.$
                                                                            .name()
                                                                            .books(
                                                                                    BookFetcher.$
                                                                                            .name()
                                                                                            .authors(
                                                                                                    AuthorFetcher.$
                                                                                                            .firstName()
                                                                                                            .lastName(),
                                                                                                    it -> it.batch(5)
                                                                                            )
                                                                            )
                                                            ),
                                                    it -> it.batch(3)
                                            )
                            )
                    );
                }),
                ctx -> {
                    ctx.sql("select tb_1_.ID, tb_1_.FIRST_NAME from AUTHOR as tb_1_");
                    ctx.statement(1).sql(
                            "select " +
                                    "tb_1_.AUTHOR_ID, " +
                                    "tb_3_.ID, tb_3_.NAME, tb_3_.STORE_ID " +
                                    "from BOOK_AUTHOR_MAPPING as tb_1_ " +
                                    "inner join BOOK as tb_3_ on tb_1_.BOOK_ID = tb_3_.ID " +
                                    "where tb_1_.AUTHOR_ID in (?, ?, ?)"
                    );
                    ctx.statement(2).sql(
                            "select " +
                                    "tb_1_.AUTHOR_ID, " +
                                    "tb_3_.ID, tb_3_.NAME, tb_3_.STORE_ID " +
                                    "from BOOK_AUTHOR_MAPPING as tb_1_ " +
                                    "inner join BOOK as tb_3_ on tb_1_.BOOK_ID = tb_3_.ID " +
                                    "where tb_1_.AUTHOR_ID in (?, ?)"
                    );
                    ctx.statement(3).sql(
                            "select tb_1_.ID, tb_1_.NAME from BOOK_STORE as tb_1_ where tb_1_.ID in (?, ?)"
                    );
                    ctx.statement(4).sql(
                            "select " +
                                    "tb_1_.STORE_ID, " +
                                    "tb_1_.ID, tb_1_.NAME " +
                                    "from BOOK as tb_1_ " +
                                    "where tb_1_.STORE_ID in (?, ?)"
                    );
                    ctx.statement(5).sql(
                            "select " +
                                    "tb_1_.BOOK_ID, " +
                                    "tb_3_.ID, tb_3_.FIRST_NAME, tb_3_.LAST_NAME " +
                                    "from BOOK_AUTHOR_MAPPING as tb_1_ " +
                                    "inner join AUTHOR as tb_3_ on tb_1_.AUTHOR_ID = tb_3_.ID " +
                                    "where tb_1_.BOOK_ID in (?, ?, ?, ?, ?)"
                    );
                    ctx.statement(6).sql(
                            "select " +
                                    "tb_1_.BOOK_ID, " +
                                    "tb_3_.ID, tb_3_.FIRST_NAME, tb_3_.LAST_NAME " +
                                    "from BOOK_AUTHOR_MAPPING as tb_1_ " +
                                    "inner join AUTHOR as tb_3_ on tb_1_.AUTHOR_ID = tb_3_.ID " +
                                    "where tb_1_.BOOK_ID in (?, ?, ?, ?, ?)"
                    );
                    ctx.statement(7).sql(
                            "select " +
                                    "tb_1_.BOOK_ID, " +
                                    "tb_3_.ID, tb_3_.FIRST_NAME, tb_3_.LAST_NAME " +
                                    "from BOOK_AUTHOR_MAPPING as tb_1_ " +
                                    "inner join AUTHOR as tb_3_ on tb_1_.AUTHOR_ID = tb_3_.ID " +
                                    "where tb_1_.BOOK_ID in (?, ?)"
                    );
                    ctx.rows(authors -> {
                        System.out.println(authors);
                    });
                }
        );
    }
}
