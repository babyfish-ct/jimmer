package org.babyfish.jimmer.sql.fetcher;

import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import static org.babyfish.jimmer.sql.common.Constants.*;
import org.babyfish.jimmer.sql.model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MultiLevelTest extends AbstractQueryTest {

    @Test
    public void testFromBookStore() {
        executeAndExpect(
                getLambdaClient().createQuery(BookStoreTable.class, (q, store) -> {
                    q.orderBy(store.name());
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
                                                                                                            .name(),
                                                                                                    it -> it.filter(args -> {
                                                                                                        args.orderBy(args.getTable().name());
                                                                                                    })
                                                                                            ),
                                                                                    it -> it.batch(3).filter(args -> {
                                                                                        args
                                                                                                .orderBy(args.getTable().name())
                                                                                                .orderBy(args.getTable().edition());
                                                                                    })
                                                                            ),
                                                                    it -> it.batch(5).filter(args -> {
                                                                        args.orderBy(args.getTable().firstName());
                                                                    })
                                                            ),
                                                    it -> it.filter(args -> {
                                                        args
                                                                .orderBy(args.getTable().name())
                                                                .orderBy(args.getTable().edition());
                                                    })
                                            )
                            )
                    );
                }),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME " +
                                    "from BOOK_STORE as tb_1_ " +
                                    "order by tb_1_.NAME asc"
                    );
                    ctx.statement(1).sql(
                            "select tb_1_.STORE_ID, tb_1_.ID, tb_1_.NAME " +
                                    "from BOOK as tb_1_ " +
                                    "where tb_1_.STORE_ID in (?, ?) " +
                                    "order by tb_1_.NAME asc, tb_1_.EDITION asc"
                    ).variables(manningId, oreillyId);
                    ctx.statement(2).sql(
                            "select " +
                                    "tb_2_.BOOK_ID, " +
                                    "tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME " +
                                    "from AUTHOR as tb_1_ " +
                                    "inner join BOOK_AUTHOR_MAPPING as tb_2_ on tb_1_.ID = tb_2_.AUTHOR_ID " +
                                    "where tb_2_.BOOK_ID in (?, ?, ?, ?, ?) " +
                                    "order by tb_1_.FIRST_NAME asc"
                    ).variables(
                            effectiveTypeScriptId1, effectiveTypeScriptId2, effectiveTypeScriptId3,
                            graphQLInActionId1, graphQLInActionId2
                    );
                    ctx.statement(3).sql(
                            "select " +
                                    "tb_2_.BOOK_ID, " +
                                    "tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME " +
                                    "from AUTHOR as tb_1_ " +
                                    "inner join BOOK_AUTHOR_MAPPING as tb_2_ on tb_1_.ID = tb_2_.AUTHOR_ID " +
                                    "where tb_2_.BOOK_ID in (?, ?, ?, ?, ?) " +
                                    "order by tb_1_.FIRST_NAME asc"
                    ).variables(
                            graphQLInActionId3,
                            learningGraphQLId1, learningGraphQLId2, learningGraphQLId3,
                            programmingTypeScriptId1
                    );
                    ctx.statement(4).sql(
                            "select " +
                                    "tb_2_.BOOK_ID, " +
                                    "tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME " +
                                    "from AUTHOR as tb_1_ " +
                                    "inner join BOOK_AUTHOR_MAPPING as tb_2_ on tb_1_.ID = tb_2_.AUTHOR_ID " +
                                    "where tb_2_.BOOK_ID in (?, ?) " +
                                    "order by tb_1_.FIRST_NAME asc"
                    ).variables(programmingTypeScriptId2, programmingTypeScriptId3);
                    ctx.statement(5).sql(
                            "select " +
                                    "tb_2_.AUTHOR_ID, " +
                                    "tb_1_.ID, tb_1_.NAME, tb_1_.STORE_ID " +
                                    "from BOOK as tb_1_ " +
                                    "inner join BOOK_AUTHOR_MAPPING as tb_2_ on tb_1_.ID = tb_2_.BOOK_ID " +
                                    "where tb_2_.AUTHOR_ID in (?, ?, ?) " +
                                    "order by tb_1_.NAME asc, tb_1_.EDITION asc"
                    ).variables(danId, sammerId, alexId);
                    ctx.statement(6).sql(
                            "select " +
                                    "tb_2_.AUTHOR_ID, " +
                                    "tb_1_.ID, tb_1_.NAME, tb_1_.STORE_ID " +
                                    "from BOOK as tb_1_ " +
                                    "inner join BOOK_AUTHOR_MAPPING as tb_2_ on tb_1_.ID = tb_2_.BOOK_ID " +
                                    "where tb_2_.AUTHOR_ID in (?, ?) " +
                                    "order by tb_1_.NAME asc, tb_1_.EDITION asc"
                    ).variables(borisId, eveId);
                    ctx.statement(7).sql(
                            "select " +
                                    "tb_1_.ID, tb_1_.NAME " +
                                    "from BOOK_STORE as tb_1_ " +
                                    "where tb_1_.ID in (?, ?) " +
                                    "order by tb_1_.NAME asc"
                    ).variables(oreillyId, manningId);

                    ctx.rows(stores -> {
                        Assertions.assertFalse(stores.isEmpty());
                        assertLoadState(stores, "id", "name", "books");
                        for (BookStore store : stores) {
                            Assertions.assertFalse(store.books().isEmpty());
                            assertLoadState(store.books(), "id", "name", "authors");
                            for (Book book : store.books()) {
                                Assertions.assertFalse(book.authors().isEmpty());
                                assertLoadState(book.authors(), "id", "firstName", "lastName", "books");
                                for (Author author : book.authors()) {
                                    Assertions.assertFalse(author.books().isEmpty());
                                    assertLoadState(author.books(), "id", "name", "store");
                                    for (Book book2 : author.books()) {
                                        assertLoadState(book2.store(), "id", "name");
                                    }
                                }
                            }
                        }
                    });
                }
        );
    }

    @Test
    public void testFromAuthor() {
        executeAndExpect(
                getLambdaClient().createQuery(AuthorTable.class, (q, author) -> {
                    q.orderBy(author.firstName());
                    return q.select(
                            author.fetch(
                                    AuthorFetcher.$
                                            .firstName()
                                            .lastName()
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
                                                                                                    it -> it.batch(5).filter(args -> {
                                                                                                        args.orderBy(args.getTable().firstName());
                                                                                                    })
                                                                                            ),
                                                                                    it -> it.filter(args -> {
                                                                                        args
                                                                                                .orderBy(args.getTable().name())
                                                                                                .orderBy(args.getTable().edition());
                                                                                    })
                                                                            ),
                                                                    it -> it.filter(args -> {
                                                                        args.orderBy(args.getTable().name());
                                                                    })
                                                            ),
                                                    it -> it.batch(3).filter(args -> {
                                                        args
                                                                .orderBy(args.getTable().name())
                                                                .orderBy(args.getTable().edition());
                                                    })
                                            )
                            )
                    );
                }),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME " +
                                    "from AUTHOR as tb_1_ " +
                                    "order by tb_1_.FIRST_NAME asc"
                    );
                    ctx.statement(1).sql(
                            "select " +
                                    "tb_2_.AUTHOR_ID, " +
                                    "tb_1_.ID, tb_1_.NAME, tb_1_.STORE_ID " +
                                    "from BOOK as tb_1_ " +
                                    "inner join BOOK_AUTHOR_MAPPING as tb_2_ on tb_1_.ID = tb_2_.BOOK_ID " +
                                    "where tb_2_.AUTHOR_ID in (?, ?, ?) " +
                                    "order by tb_1_.NAME asc, tb_1_.EDITION asc"
                    ).variables(alexId, borisId, danId);
                    ctx.statement(2).sql(
                            "select " +
                                    "tb_2_.AUTHOR_ID, " +
                                    "tb_1_.ID, tb_1_.NAME, tb_1_.STORE_ID " +
                                    "from BOOK as tb_1_ " +
                                    "inner join BOOK_AUTHOR_MAPPING as tb_2_ on tb_1_.ID = tb_2_.BOOK_ID " +
                                    "where tb_2_.AUTHOR_ID in (?, ?) " +
                                    "order by tb_1_.NAME asc, tb_1_.EDITION asc"
                    ).variables(eveId, sammerId);
                    ctx.statement(3).sql(
                            "select tb_1_.ID, tb_1_.NAME " +
                                    "from BOOK_STORE as tb_1_ " +
                                    "where tb_1_.ID in (?, ?) " +
                                    "order by tb_1_.NAME asc"
                    ).variables(oreillyId, manningId);
                    ctx.statement(4).sql(
                            "select " +
                                    "tb_1_.STORE_ID, " +
                                    "tb_1_.ID, tb_1_.NAME " +
                                    "from BOOK as tb_1_ " +
                                    "where tb_1_.STORE_ID in (?, ?) " +
                                    "order by tb_1_.NAME asc, tb_1_.EDITION asc"
                    ).variables(manningId, oreillyId);
                    ctx.statement(5).sql(
                            "select " +
                                    "tb_2_.BOOK_ID, " +
                                    "tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME " +
                                    "from AUTHOR as tb_1_ " +
                                    "inner join BOOK_AUTHOR_MAPPING as tb_2_ on tb_1_.ID = tb_2_.AUTHOR_ID " +
                                    "where tb_2_.BOOK_ID in (?, ?, ?, ?, ?) " +
                                    "order by tb_1_.FIRST_NAME asc"
                    ).variables(
                            effectiveTypeScriptId1, effectiveTypeScriptId2, effectiveTypeScriptId3,
                            graphQLInActionId1, graphQLInActionId2
                    );
                    ctx.statement(6).sql(
                            "select " +
                                    "tb_2_.BOOK_ID, " +
                                    "tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME " +
                                    "from AUTHOR as tb_1_ " +
                                    "inner join BOOK_AUTHOR_MAPPING as tb_2_ on tb_1_.ID = tb_2_.AUTHOR_ID " +
                                    "where tb_2_.BOOK_ID in (?, ?, ?, ?, ?) " +
                                    "order by tb_1_.FIRST_NAME asc"
                    ).variables(
                            graphQLInActionId3,
                            learningGraphQLId1, learningGraphQLId2, learningGraphQLId3,
                            programmingTypeScriptId1
                    );
                    ctx.statement(7).sql(
                            "select " +
                                    "tb_2_.BOOK_ID, " +
                                    "tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME " +
                                    "from AUTHOR as tb_1_ " +
                                    "inner join BOOK_AUTHOR_MAPPING as tb_2_ on tb_1_.ID = tb_2_.AUTHOR_ID " +
                                    "where tb_2_.BOOK_ID in (?, ?) " +
                                    "order by tb_1_.FIRST_NAME asc"
                    ).variables(programmingTypeScriptId2, programmingTypeScriptId3);

                    ctx.rows(authors -> {
                       Assertions.assertFalse(authors.isEmpty());
                       assertLoadState(authors, "id", "firstName", "lastName", "books");
                       for (Author author : authors) {
                           Assertions.assertFalse(author.books().isEmpty());
                           assertLoadState(author.books(), "id", "name", "store");
                           for (Book book : author.books()) {
                               assertLoadState(book.store(), "id", "name", "books");
                               Assertions.assertFalse(book.store().books().isEmpty());
                               assertLoadState(book.store().books(), "id", "name", "authors");
                               for (Book book2 : book.store().books()) {
                                   Assertions.assertFalse(book2.authors().isEmpty());
                                   assertLoadState(book2.authors(), "id", "firstName", "lastName");
                               }
                           }
                       }
                    });
                }
        );
    }
}
