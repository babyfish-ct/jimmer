package org.babyfish.jimmer.sql.query;

import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import static org.babyfish.jimmer.sql.common.Constants.*;

import org.babyfish.jimmer.sql.model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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
                            Assertions.assertTrue(((ImmutableSpi) book).__isLoaded("id"));
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
                            "select tb_1_.ID, tb_1_.ID, tb_1_.NAME, tb_1_.WEBSITE, tb_1_.VERSION " +
                                    "from BOOK_STORE as tb_1_ " +
                                    "where tb_1_.ID in (?, ?)");
                    ctx.statement(1).variables(oreillyId, manningId);
                    ctx.rows(books -> {
                        Assertions.assertEquals(12, books.size());
                        for (Book book : books) {

                            Assertions.assertTrue(((ImmutableSpi) book).__isLoaded("id"));
                            Assertions.assertTrue(((ImmutableSpi) book).__isLoaded("name"));
                            Assertions.assertTrue(((ImmutableSpi) book).__isLoaded("store"));
                            Assertions.assertFalse(((ImmutableSpi) book).__isLoaded("edition"));
                            Assertions.assertFalse(((ImmutableSpi) book).__isLoaded("price"));
                            Assertions.assertFalse(((ImmutableSpi) book).__isLoaded("authors"));

                            BookStore store = book.store();
                            Assertions.assertTrue(((ImmutableSpi) store).__isLoaded("name"));
                        }
                    });
                }
        );
    }
}
