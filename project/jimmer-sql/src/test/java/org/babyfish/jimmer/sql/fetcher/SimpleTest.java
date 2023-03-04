package org.babyfish.jimmer.sql.fetcher;

import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import static org.babyfish.jimmer.sql.common.Constants.*;

import org.babyfish.jimmer.sql.model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SimpleTest extends AbstractQueryTest {

    @Test
    public void testFetchScalar() {
        executeAndExpect(
                getLambdaClient().createQuery(BookTable.class, (q, book) -> {
                    return q.select(
                            book.fetch(
                                    BookFetcher.$.name()
                            )
                    );
                }),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME from BOOK as tb_1_"
                    );
                    ctx.rows(books -> {
                        Assertions.assertEquals(12, books.size());
                        assertLoadState(
                                books,
                                "id", "name"
                        );
                    });
                }
        );
    }

    @Test
    public void testFetchManyToOne() {
        executeAndExpect(
                getLambdaClient().createQuery(BookTable.class, (q, book) -> {
                    q.orderBy(book.name()).orderBy(book.edition());
                    return q.select(
                            book.fetch(
                                    BookFetcher.$
                                            .name()
                                            .store(
                                                    BookStoreFetcher.$.name(),
                                                    it -> it.filter(args -> {
                                                        args.orderBy(args.getTable().name());
                                                    })
                                            )
                            )
                    );
                }),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.STORE_ID " +
                                    "from BOOK as tb_1_ " +
                                    "order by tb_1_.NAME asc, tb_1_.EDITION asc"
                    );
                    ctx.statement(1).sql(
                            "select tb_1_.ID, tb_1_.NAME " +
                                    "from BOOK_STORE as tb_1_ " +
                                    "where tb_1_.ID in (?, ?) " +
                                    "order by tb_1_.NAME asc"
                    ).variables(oreillyId, manningId);
                    ctx.rows(books -> {
                        Assertions.assertEquals(12, books.size());
                        assertLoadState(books, "id", "name", "store");
                        for (Book book : books) {
                            assertLoadState(book.store(), "id", "name");
                        }
                    });
                }
        );
    }

    @Test
    public void fetchOneToMany() {
        executeAndExpect(
                getLambdaClient().createQuery(BookStoreTable.class, (q, book) -> {
                    return q.select(
                            book.fetch(
                                    BookStoreFetcher.$
                                            .name()
                                            .books(
                                                    BookFetcher.$.name()
                                            )
                            )
                    );
                }),
                ctx -> {
                    ctx.sql("select tb_1_.ID, tb_1_.NAME from BOOK_STORE as tb_1_");
                    ctx.statement(1).sql(
                            "select " +
                                    "tb_1_.STORE_ID, " +
                                    "tb_1_.ID, tb_1_.NAME " +
                                    "from BOOK as tb_1_ " +
                                    "where tb_1_.STORE_ID in (?, ?)"
                    );
                    ctx.rows(stores -> {
                        Assertions.assertEquals(2, stores.size());
                        assertLoadState(stores, "id", "name", "books");
                        for (BookStore store : stores) {
                            Assertions.assertEquals(
                                    store.name().equals("MANNING") ? 3 : 9,
                                    store.books().size()
                            );
                            assertLoadState(store.books(), "id", "name");
                        }
                    });
                }
        );
    }

    @Test
    public void testLoadManyToManyWithOnlyId() {
        executeAndExpect(
                getLambdaClient().createQuery(BookTable.class, (q, book) -> {
                    return q.select(
                            book.fetch(
                                    BookFetcher.$
                                            .name()
                                            .authors()
                            )
                    );
                }),
                ctx -> {
                    ctx.sql("select tb_1_.ID, tb_1_.NAME from BOOK as tb_1_");
                    ctx.statement(1).sql(
                            "select tb_1_.BOOK_ID, tb_1_.AUTHOR_ID " +
                                    "from BOOK_AUTHOR_MAPPING as tb_1_ " +
                                    "where tb_1_.BOOK_ID in (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                    );
                    ctx.rows(books -> {
                        Assertions.assertEquals(12, books.size());
                        assertLoadState(books, "id", "name", "authors");
                        for (Book book : books) {
                            Assertions.assertFalse(book.authors().isEmpty());
                            assertLoadState(book.authors(), "id");
                        }
                    });
                }
        );
    }

    @Test
    public void testLoadManyToMany() {
        executeAndExpect(
                getLambdaClient().createQuery(BookTable.class, (q, book) -> {
                    return q.select(
                            book.fetch(
                                    BookFetcher.$
                                            .name()
                                            .authors(
                                                    AuthorFetcher.$.firstName().lastName(),
                                                    it -> it.batch(6)
                                            )
                            )
                    );
                }),
                ctx -> {
                    ctx.sql("select tb_1_.ID, tb_1_.NAME from BOOK as tb_1_");
                    ctx.statement(1).sql(
                            "select " +
                                    "tb_2_.BOOK_ID, " +
                                    "tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME " +
                                    "from AUTHOR as tb_1_ " +
                                    "inner join BOOK_AUTHOR_MAPPING as tb_2_ on tb_1_.ID = tb_2_.AUTHOR_ID " +
                                    "where tb_2_.BOOK_ID in (?, ?, ?, ?, ?, ?)"
                    );
                    ctx.statement(2).sql(
                            "select " +
                                    "tb_2_.BOOK_ID, " +
                                    "tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME " +
                                    "from AUTHOR as tb_1_ " +
                                    "inner join BOOK_AUTHOR_MAPPING as tb_2_ on tb_1_.ID = tb_2_.AUTHOR_ID " +
                                    "where tb_2_.BOOK_ID in (?, ?, ?, ?, ?, ?)"
                    );
                    ctx.rows(books -> {
                        Assertions.assertEquals(12, books.size());
                        assertLoadState(books, "id", "name", "authors");
                        for (Book book : books) {
                            Assertions.assertFalse(book.authors().isEmpty());
                            assertLoadState(book.authors(), "id", "firstName", "lastName");
                        }
                    });
                }
        );
    }

    @Test
    public void testLoadInverseManyToManyWithOnlyId() {
        executeAndExpect(
                getLambdaClient().createQuery(AuthorTable.class, (q, author) -> {
                    return q.select(
                            author.fetch(
                                    AuthorFetcher.$
                                            .firstName()
                                            .lastName()
                                            .books()
                            )
                    );
                }),
                ctx -> {
                    ctx.sql("select tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME from AUTHOR as tb_1_");
                    ctx.statement(1).sql(
                            "select tb_1_.AUTHOR_ID, tb_1_.BOOK_ID " +
                                    "from BOOK_AUTHOR_MAPPING as tb_1_ " +
                                    "where tb_1_.AUTHOR_ID in (?, ?, ?, ?, ?)"
                    );
                    ctx.rows(authors -> {
                        Assertions.assertEquals(5, authors.size());
                        assertLoadState(authors, "id", "firstName", "lastName", "books");
                        for (Author author : authors) {
                            Assertions.assertFalse(author.books().isEmpty());
                            assertLoadState(author.books(), "id");
                        }
                    });
                }
        );
    }

    @Test
    public void testLoadInverseManyToMany() {
        executeAndExpect(
                getLambdaClient().createQuery(AuthorTable.class, (q, author) -> {
                    return q.select(
                            author.fetch(
                                    AuthorFetcher.$
                                            .firstName()
                                            .lastName()
                                            .books(
                                                    BookFetcher.$.name()
                                            )
                            )
                    );
                }),
                ctx -> {
                    ctx.sql("select tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME from AUTHOR as tb_1_");
                    ctx.statement(1).sql(
                            "select " +
                                    "tb_2_.AUTHOR_ID, " +
                                    "tb_1_.ID, tb_1_.NAME " +
                                    "from BOOK as tb_1_ " +
                                    "inner join BOOK_AUTHOR_MAPPING as tb_2_ on tb_1_.ID = tb_2_.BOOK_ID " +
                                    "where tb_2_.AUTHOR_ID in (?, ?, ?, ?, ?)"
                    );
                    ctx.rows(authors -> {
                        Assertions.assertEquals(5, authors.size());
                        assertLoadState(authors, "id", "firstName", "lastName", "books");
                        for (Author author : authors) {
                            Assertions.assertFalse(author.books().isEmpty());
                            assertLoadState(author.books(), "id", "name");
                        }
                    });
                }
        );
    }

    @Test
    public void testJavaFormula() {
        AuthorTable table = AuthorTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.id().eq(eveId))
                        .select(
                                table.fetch(
                                        AuthorFetcher.$.fullName()
                                )
                        ),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME " +
                                    "from AUTHOR as tb_1_ " +
                                    "where tb_1_.ID = ?"
                    );
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":\"fd6bb6cf-336d-416c-8005-1ae11a6694b5\"," +
                                    "--->--->\"fullName\":\"Eve Procello\"" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testSqlFormula() {
        AuthorTable table = AuthorTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.id().eq(eveId))
                        .select(
                                table.fetch(
                                        AuthorFetcher.$.fullName2()
                                )
                        ),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, concat(tb_1_.FIRST_NAME, ' ', tb_1_.LAST_NAME) " +
                                    "from AUTHOR as tb_1_ " +
                                    "where tb_1_.ID = ?");
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":\"fd6bb6cf-336d-416c-8005-1ae11a6694b5\"," +
                                    "--->--->\"fullName2\":\"Eve Procello\"" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testIdView() {
        BookTable table = BookTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.id().eq(graphQLInActionId3))
                        .select(
                                table.fetch(
                                        BookFetcher.$
                                                .allScalarFields()
                                                .storeId()
                                                .authorIds()
                                )
                        ),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID from BOOK as tb_1_ where tb_1_.ID = ?"
                    );
                    ctx.statement(1).sql(
                            "select tb_1_.AUTHOR_ID from BOOK_AUTHOR_MAPPING as tb_1_ where tb_1_.BOOK_ID = ?"
                    );
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":\"780bdf07-05af-48bf-9be9-f8c65236fecc\"," +
                                    "--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->\"edition\":3," +
                                    "--->--->\"price\":80.00," +
                                    "--->--->\"storeId\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"," +
                                    "--->--->\"authorIds\":[\"eb4963fd-5223-43e8-b06b-81e6172ee7ae\"]" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }
}
