package org.babyfish.jimmer.sql.query;

import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import static org.babyfish.jimmer.sql.common.Constants.*;

import org.babyfish.jimmer.sql.model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class FetcherQuery extends AbstractQueryTest {

    @Test
    public void testFetchScalar() {
        executeAndExpect(
                getSqlClient().createQuery(BookTable.class, (q, book) -> {
                    return q.select(
                            book.fetch(
                                    BookFetcher.$.name()
                            )
                    );
                }),
                ctx -> {
                    ctx.sql("select tb_1_.ID, tb_1_.NAME from BOOK as tb_1_");
                    ctx.rows(books -> {
                        Assertions.assertEquals(12, books.size());
                        for (Book book : books) {
                            Assertions.assertTrue(((ImmutableSpi) book).__isLoaded("name"));
                            Assertions.assertFalse(((ImmutableSpi) book).__isLoaded("edition"));
                            Assertions.assertFalse(((ImmutableSpi) book).__isLoaded("price"));
                            Assertions.assertFalse(((ImmutableSpi) book).__isLoaded("store"));
                            Assertions.assertFalse(((ImmutableSpi) book).__isLoaded("authors"));
                        }
                    });
                }
        );
    }

    @Test
    public void testFetchManyToOne() {
        executeAndExpect(
                getSqlClient().createQuery(BookTable.class, (q, book) -> {
                    return q.select(
                            book.fetch(
                                    BookFetcher.$
                                            .name()
                                            .store(
                                                    BookStoreFetcher.$.name()
                                            )
                            )
                    );
                }),
                ctx -> {
                    ctx.sql("select tb_1_.ID, tb_1_.NAME, tb_1_.STORE_ID from BOOK as tb_1_");
                    ctx.statement(1).sql(
                            "select tb_1_.ID, tb_1_.NAME " +
                                    "from BOOK_STORE as tb_1_ " +
                                    "where tb_1_.ID in (?, ?)");
                    ctx.statement(1).variables(oreillyId, manningId);
                    ctx.rows(books -> {
                        Assertions.assertEquals(12, books.size());
                        for (Book book : books) {

                            Assertions.assertTrue(((ImmutableSpi) book).__isLoaded("name"));
                            Assertions.assertFalse(((ImmutableSpi) book).__isLoaded("edition"));
                            Assertions.assertFalse(((ImmutableSpi) book).__isLoaded("price"));
                            Assertions.assertTrue(((ImmutableSpi) book).__isLoaded("store"));
                            Assertions.assertFalse(((ImmutableSpi) book).__isLoaded("authors"));

                            BookStore store = book.store();
                            Assertions.assertTrue(((ImmutableSpi) store).__isLoaded("name"));
                            Assertions.assertFalse(((ImmutableSpi) store).__isLoaded("website"));
                            Assertions.assertFalse(((ImmutableSpi) store).__isLoaded("version"));
                            Assertions.assertFalse(((ImmutableSpi) store).__isLoaded("books"));
                        }
                    });
                }
        );
    }

    @Test
    public void fetchOneToMany() {
        executeAndExpect(
                getSqlClient().createQuery(BookStoreTable.class, (q, book) -> {
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
                        for (BookStore store : stores) {

                            Assertions.assertTrue(((ImmutableSpi) store).__isLoaded("name"));
                            Assertions.assertFalse(((ImmutableSpi) store).__isLoaded("website"));
                            Assertions.assertFalse(((ImmutableSpi) store).__isLoaded("version"));
                            Assertions.assertTrue(((ImmutableSpi) store).__isLoaded("books"));

                            List<Book> books = store.books();
                            Assertions.assertFalse(books.isEmpty());
                            for (Book book : books) {
                                Assertions.assertTrue(((ImmutableSpi) book).__isLoaded("name"));
                                Assertions.assertFalse(((ImmutableSpi) book).__isLoaded("edition"));
                                Assertions.assertFalse(((ImmutableSpi) book).__isLoaded("price"));
                                Assertions.assertFalse(((ImmutableSpi) book).__isLoaded("store"));
                                Assertions.assertFalse(((ImmutableSpi) book).__isLoaded("authors"));
                            }
                        }
                    });
                }
        );
    }

    @Test
    public void testLoadManyToManyWithOnlyId() {
        executeAndExpect(
                getSqlClient().createQuery(BookTable.class, (q, book) -> {
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
                        System.out.println(books);
                    });
                }
        );
    }

    @Test
    public void testLoadManyToMany() {
        executeAndExpect(
                getSqlClient().createQuery(BookTable.class, (q, book) -> {
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
                                    "tb_1_.BOOK_ID, " +
                                    "tb_3_.ID, tb_3_.FIRST_NAME, tb_3_.LAST_NAME " +
                                    "from BOOK_AUTHOR_MAPPING as tb_1_ " +
                                    "inner join AUTHOR as tb_3_ on tb_1_.AUTHOR_ID = tb_3_.ID " +
                                    "where tb_1_.BOOK_ID in (?, ?, ?, ?, ?, ?)"
                    );
                    ctx.statement(2).sql(
                            "select " +
                                    "tb_1_.BOOK_ID, " +
                                    "tb_3_.ID, tb_3_.FIRST_NAME, tb_3_.LAST_NAME " +
                                    "from BOOK_AUTHOR_MAPPING as tb_1_ " +
                                    "inner join AUTHOR as tb_3_ on tb_1_.AUTHOR_ID = tb_3_.ID " +
                                    "where tb_1_.BOOK_ID in (?, ?, ?, ?, ?, ?)"
                    );
                    ctx.rows(books -> {
                        System.out.println(books);
                    });
                }
        );
    }
}
