package org.babyfish.jimmer.sql.query;

import org.babyfish.jimmer.Immutable;
import org.babyfish.jimmer.sql.ast.query.OrderMode;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import static org.babyfish.jimmer.sql.common.Constants.*;

import org.babyfish.jimmer.sql.model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

public class AssociationLoaderTest extends AbstractQueryTest {

    @Test
    public void loadManyToOne() {
        Book book = BookDraft.$.produce(bookDraft -> {
            bookDraft
                    .setId(graphQLInActionId1)
                    .applyStore(store -> store.setId(manningId));
        });
        anyAndExpect(
                getSqlClient()
                        .getLoaders()
                        .reference(BookTable.class, BookTable::store)
                        .loadCommand(book),
                ctx -> {
                    ctx.sql(
                            "select " +
                                    "tb_1_.ID, tb_1_.NAME, tb_1_.WEBSITE, tb_1_.VERSION " +
                                    "from BOOK_STORE tb_1_ " +
                                    "where tb_1_.ID = ?"
                    );
                    ctx.variables(manningId);
                    ctx.rows(1);
                    ctx.row(0, store -> {
                        expect(
                                "{" +
                                        "--->\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"," +
                                        "--->\"name\":\"MANNING\"," +
                                        "--->\"website\":null," +
                                        "--->\"version\":0" +
                                        "}",
                                store
                        );
                    });
                }
        );
    }

    @Test
    public void batchLoadManyToOne() {
        List<Book> books = Arrays.asList(
                BookDraft.$.produce(book -> {
                    book
                            .setId(graphQLInActionId1)
                            .applyStore(store -> store.setId(manningId));
                }),
                BookDraft.$.produce(book -> {
                    book
                            .setId(learningGraphQLId1)
                            .applyStore(store -> store.setId(oreillyId));
                })
        );
        anyAndExpect(
                getSqlClient()
                        .getLoaders()
                        .reference(BookTable.class, BookTable::store)
                        .batchLoadCommand(books),
                ctx -> {
                    ctx.sql(
                            "select " +
                                    "tb_1_.ID, tb_1_.NAME, tb_1_.WEBSITE, tb_1_.VERSION " +
                                    "from BOOK_STORE tb_1_ " +
                                    "where tb_1_.ID in (?, ?)"
                    );
                    ctx.variables(manningId, oreillyId);
                    ctx.rows(1);
                    ctx.row(0, map -> {
                        expect(
                               "{" +
                                       "--->\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"," +
                                       "--->\"name\":\"MANNING\"," +
                                       "--->\"website\":null," +
                                       "--->\"version\":0" +
                                       "}",
                               map.get(books.get(0))
                        );
                        expect(
                                "{" +
                                        "--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"," +
                                        "--->\"name\":\"O'REILLY\"," +
                                        "--->\"website\":null," +
                                        "--->\"version\":0" +
                                        "}",
                                map.get(books.get(1))
                        );
                    });
                }
        );
    }

    @Test
    public void testLoadOneToMany() {
        anyAndExpect(
                getSqlClient()
                        .getLoaders()
                        .list(
                                BookStoreTableEx.class,
                                BookStoreTableEx::books
                        )
                        .forFilter(args -> {
                            args.orderBy(args.getTable().edition().desc());
                        })
                        .loadCommand(
                                BookStoreDraft.$.produce(store -> {
                                    store.setId(manningId);
                                }),
                                2,
                                0
                        ),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                    "from BOOK tb_1_ " +
                                    "where tb_1_.STORE_ID = ? " +
                                    "order by tb_1_.EDITION desc " +
                                    "limit ?"
                    );
                    ctx.variables(manningId, 2);
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":\"780bdf07-05af-48bf-9be9-f8c65236fecc\"," +
                                    "--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->\"edition\":3," +
                                    "--->--->\"price\":80.00," +
                                    "--->--->\"store\":{\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"}" +
                                    "--->}," +
                                    "--->{" +
                                    "--->--->\"id\":\"e37a8344-73bb-4b23-ba76-82eac11f03e6\"," +
                                    "--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->\"edition\":2," +
                                    "--->--->\"price\":81.00," +
                                    "--->--->\"store\":{\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"}" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testBatchLoadOneToMany() {
        List<BookStore> bookStores = Arrays.asList(
                BookStoreDraft.$.produce(store -> {
                    store.setId(oreillyId);
                }),
                BookStoreDraft.$.produce(store -> {
                    store.setId(manningId);
                })
        );
        anyAndExpect(
                getSqlClient()
                        .getLoaders()
                        .list(
                                BookStoreTableEx.class,
                                BookStoreTableEx::books
                        )
                        .forFilter(args -> {
                            args.where(args.getTable().edition().eq(3));
                        })
                        .batchLoadCommand(bookStores),
                ctx -> {
                    ctx.sql(
                            "select " +
                                    "tb_1_.STORE_ID, " +
                                    "tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                    "from BOOK tb_1_ " +
                                    "where tb_1_.STORE_ID in (?, ?) " +
                                    "and tb_1_.EDITION = ?"
                    );
                    ctx.variables(oreillyId, manningId, 3);
                    ctx.rows(1);
                    ctx.row(0, map -> {
                        expect(
                                "[" +
                                        "--->{" +
                                        "--->--->\"id\":\"64873631-5d82-4bae-8eb8-72dd955bfc56\"," +
                                        "--->--->\"name\":\"Learning GraphQL\"," +
                                        "--->--->\"edition\":3," +
                                        "--->--->\"price\":51.00," +
                                        "--->--->\"store\":{\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"}" +
                                        "--->}, {" +
                                        "--->--->\"id\":\"9eded40f-6d2e-41de-b4e7-33a28b11c8b6\"," +
                                        "--->--->\"name\":\"Effective TypeScript\"," +
                                        "--->--->\"edition\":3," +
                                        "--->--->\"price\":88.00," +
                                        "--->--->\"store\":{\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"}" +
                                        "--->}, {" +
                                        "--->--->\"id\":\"782b9a9d-eac8-41c4-9f2d-74a5d047f45a\"," +
                                        "--->--->\"name\":\"Programming TypeScript\"," +
                                        "--->--->\"edition\":3," +
                                        "--->--->\"price\":48.00," +
                                        "--->--->\"store\":{\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"}" +
                                        "--->}" +
                                        "]",
                                map.get(bookStores.get(0))
                        );
                        expect(
                                "[" +
                                        "--->{" +
                                        "--->--->\"id\":\"780bdf07-05af-48bf-9be9-f8c65236fecc\"," +
                                        "--->--->\"name\":\"GraphQL in Action\"," +
                                        "--->--->\"edition\":3," +
                                        "--->--->\"price\":80.00," +
                                        "--->--->\"store\":{\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"}" +
                                        "--->}" +
                                        "]",
                                map.get(bookStores.get(1))
                        );
                    });
                }
        );
    }

    @Test
    public void loadManyToMany() {
        anyAndExpect(
                getSqlClient()
                        .getLoaders()
                        .list(
                                BookTableEx.class,
                                BookTableEx::authors
                        )
                        .loadCommand(
                                BookDraft.$.produce(book -> {
                                    book.setId(learningGraphQLId3);
                                })
                        ),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME, tb_1_.GENDER " +
                                    "from AUTHOR tb_1_ " +
                                    "inner join BOOK_AUTHOR_MAPPING tb_2_ on tb_1_.ID = tb_2_.AUTHOR_ID " +
                                    "where tb_2_.BOOK_ID = ?"
                    );
                    ctx.variables(learningGraphQLId3);
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":\"1e93da94-af84-44f4-82d1-d8a9fd52ea94\"," +
                                    "--->--->\"firstName\":\"Alex\"," +
                                    "--->--->\"lastName\":\"Banks\"," +
                                    "--->--->\"fullName\":\"Alex Banks\"," +
                                    "--->--->\"gender\":\"MALE\"" +
                                    "--->}," +
                                    "--->{" +
                                    "--->--->\"id\":\"fd6bb6cf-336d-416c-8005-1ae11a6694b5\"," +
                                    "--->--->\"firstName\":\"Eve\"," +
                                    "--->--->\"lastName\":\"Procello\"," +
                                    "--->--->\"fullName\":\"Eve Procello\"," +
                                    "--->--->\"gender\":\"FEMALE\"" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void batchLoadManyToMany() {
        List<Book> books = Arrays.asList(
                BookDraft.$.produce(book -> {
                    book.setId(learningGraphQLId3);
                }),
                BookDraft.$.produce(book -> {
                    book.setId(graphQLInActionId3);
                })
        );
        anyAndExpect(
                getSqlClient()
                        .getLoaders()
                        .list(
                                BookTableEx.class,
                                BookTableEx::authors
                        )
                        .forFilter(args -> args.orderBy(args.getTable().firstName()))
                        .batchLoadCommand(books),
                ctx -> {
                    ctx.sql(
                            "select " +
                                    "tb_2_.BOOK_ID, " +
                                    "tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME, tb_1_.GENDER " +
                                    "from AUTHOR tb_1_ " +
                                    "inner join BOOK_AUTHOR_MAPPING tb_2_ on tb_1_.ID = tb_2_.AUTHOR_ID " +
                                    "where tb_2_.BOOK_ID in (?, ?) " +
                                    "order by tb_1_.FIRST_NAME asc"
                    );
                    ctx.variables(learningGraphQLId3, graphQLInActionId3);
                    ctx.rows(rows -> {
                        Assertions.assertEquals(1, rows.size());
                        ctx.rows(1);
                        ctx.row(0, map -> {
                            expect(
                                    "[" +
                                            "--->{" +
                                            "--->--->\"id\":\"1e93da94-af84-44f4-82d1-d8a9fd52ea94\"," +
                                            "--->--->\"firstName\":\"Alex\"," +
                                            "--->--->\"lastName\":\"Banks\"," +
                                            "--->--->\"fullName\":\"Alex Banks\"," +
                                            "--->--->\"gender\":\"MALE\"" +
                                            "--->}, " +
                                            "--->{" +
                                            "--->--->\"id\":\"fd6bb6cf-336d-416c-8005-1ae11a6694b5\"," +
                                            "--->--->\"firstName\":\"Eve\"," +
                                            "--->--->\"lastName\":\"Procello\"," +
                                            "--->--->\"fullName\":\"Eve Procello\"," +
                                            "--->--->\"gender\":\"FEMALE\"" +
                                            "--->}" +
                                            "]",
                                    map.get(books.get(0))
                            );
                        });
                        ctx.row(0, map -> {
                            expect(
                                    "[" +
                                            "--->{" +
                                            "--->--->\"id\":\"eb4963fd-5223-43e8-b06b-81e6172ee7ae\"," +
                                            "--->--->\"firstName\":\"Samer\"," +
                                            "--->--->\"lastName\":\"Buna\"," +
                                            "--->--->\"fullName\":\"Samer Buna\"," +
                                            "--->--->\"gender\":\"MALE\"" +
                                            "--->}" +
                                            "]",
                                    map.get(books.get(1))
                            );
                        });
                    });
                }
        );
    }
}
