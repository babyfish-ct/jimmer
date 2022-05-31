package org.babyfish.jimmer.sql.query;

import org.babyfish.jimmer.sql.ast.query.OrderMode;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import static org.babyfish.jimmer.sql.common.Constants.*;

import org.babyfish.jimmer.sql.model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class AssociationLoaderTest extends AbstractQueryTest {

    @Test
    public void loadManyToOne() {
        anyAndExpect(
                getSqlClient()
                        .getReferenceLoader(BookTable.class, BookTable::store)
                        .loadCommand(
                                BookDraft.$.produce(book -> {
                                    book.setStore(store -> store.setId(manningId));
                                })
                        ),
                ctx -> {
                    ctx.sql(
                            "select " +
                                    "tb_1_.ID, tb_1_.NAME, tb_1_.WEBSITE, tb_1_.VERSION " +
                                    "from BOOK_STORE as tb_1_ " +
                                    "where tb_1_.ID = ?"
                    );
                    ctx.variables(manningId);
                    ctx.rows(rows -> {
                        Assertions.assertEquals(1, rows.size());
                        String parentText = "{" +
                                "--->\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"," +
                                "--->\"name\":\"MANNING\"," +
                                "--->\"website\":null," +
                                "--->\"version\":0" +
                                "}";
                        Assertions.assertEquals(
                                parentText.replace("--->", ""),
                                rows.get(0).toString()
                        );
                    });
                }
        );
    }

    @Test
    public void batchLoadManyToOne() {
        anyAndExpect(
                getSqlClient()
                        .getReferenceLoader(BookTable.class, BookTable::store)
                        .batchLoadCommand(
                                Arrays.asList(
                                    BookDraft.$.produce(book -> {
                                        book.setStore(store -> store.setId(manningId));
                                    }),
                                    BookDraft.$.produce(book -> {
                                        book.setStore(store -> store.setId(oreillyId));
                                    })
                                )
                        ),
                ctx -> {
                    ctx.sql(
                            "select " +
                                    "tb_1_.ID, tb_1_.NAME, tb_1_.WEBSITE, tb_1_.VERSION " +
                                    "from BOOK_STORE as tb_1_ " +
                                    "where tb_1_.ID in (?, ?)"
                    );
                    ctx.variables(manningId, oreillyId);
                    ctx.rows(rows -> {
                        Assertions.assertEquals(1, rows.size());
                        String paren1Text =
                                "{" +
                                "--->\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"," +
                                "--->\"name\":\"MANNING\"," +
                                "--->\"website\":null," +
                                "--->\"version\":0" +
                                "}";
                        String paren2Text =
                                "{" +
                                "--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"," +
                                "--->\"name\":\"O'REILLY\"," +
                                "--->\"website\":null," +
                                "--->\"version\":0" +
                                "}";
                        Assertions.assertEquals(
                                paren1Text.replace("--->", ""),
                                rows.get(0).get(
                                        BookDraft.$.produce(book -> {
                                            book.store(true).setId(manningId);
                                        })
                                ).toString()
                        );
                        Assertions.assertEquals(
                                paren2Text.replace("--->", ""),
                                rows.get(0).get(
                                        BookDraft.$.produce(book -> {
                                            book.store(true).setId(oreillyId);
                                        })
                                ).toString()
                        );
                    });
                }
        );
    }

    @Test
    public void testLoadOneToMany() {
        anyAndExpect(
                getSqlClient()
                        .getListLoader(
                                BookStoreTableEx.class,
                                BookStoreTableEx::books,
                                (q, t) -> {
                                    q.orderBy(t.edition(), OrderMode.DESC);
                                }
                        )
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
                                    "from BOOK as tb_1_ " +
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
        anyAndExpect(
                getSqlClient()
                        .getListLoader(
                                BookStoreTableEx.class,
                                BookStoreTableEx::books,
                                (q, t) -> {
                                    q.where(t.edition().eq(3));
                                }
                        )
                        .batchLoadCommand(
                                Arrays.asList(
                                        BookStoreDraft.$.produce(store -> {
                                            store.setId(oreillyId);
                                        }),
                                        BookStoreDraft.$.produce(store -> {
                                            store.setId(manningId);
                                        })
                                )
                        ),
                ctx -> {
                    ctx.sql(
                            "select " +
                                    "tb_1_.STORE_ID, " +
                                    "tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                    "from BOOK as tb_1_ " +
                                    "where tb_1_.STORE_ID in (?, ?) " +
                                    "and tb_1_.EDITION = ?"
                    );
                    ctx.variables(oreillyId, manningId, 3);
                    ctx.rows(rows -> {
                        Assertions.assertEquals(1, rows.size());
                        String manningBookListText =
                                "[" +
                                        "--->{" +
                                        "--->--->\"id\":\"780bdf07-05af-48bf-9be9-f8c65236fecc\"," +
                                        "--->--->\"name\":\"GraphQL in Action\"," +
                                        "--->--->\"edition\":3," +
                                        "--->--->\"price\":80.00," +
                                        "--->--->\"store\":{\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"}" +
                                        "--->}" +
                                        "]";
                        Assertions.assertEquals(
                                manningBookListText.replace("--->", ""),
                                rows.get(0).get(
                                        BookStoreDraft.$.produce(store -> {
                                            store.setId(manningId);
                                        })
                                ).toString()
                        );
                    });
                }
        );
    }

    @Test
    public void loadManyToMany() {
        anyAndExpect(
                getSqlClient()
                        .getListLoader(
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
                            "select tb_1_.AUTHOR_ID, tb_3_.FIRST_NAME, tb_3_.LAST_NAME, tb_3_.GENDER " +
                                    "from BOOK_AUTHOR_MAPPING as tb_1_ " +
                                    "inner join AUTHOR as tb_3_ on tb_1_.AUTHOR_ID = tb_3_.ID " +
                                    "where tb_1_.BOOK_ID = ?"
                    );
                    ctx.variables(learningGraphQLId3);
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":\"1e93da94-af84-44f4-82d1-d8a9fd52ea94\"," +
                                    "--->--->\"firstName\":\"Alex\"," +
                                    "--->--->\"lastName\":\"Banks\"," +
                                    "--->--->\"gender\":\"MALE\"" +
                                    "--->}," +
                                    "--->{" +
                                    "--->--->\"id\":\"fd6bb6cf-336d-416c-8005-1ae11a6694b5\"," +
                                    "--->--->\"firstName\":\"Eve\"," +
                                    "--->--->\"lastName\":\"Procello\"," +
                                    "--->--->\"gender\":\"MALE\"" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void batchLoadManyToMany() {
        anyAndExpect(
                getSqlClient()
                        .getListLoader(
                                BookTableEx.class,
                                BookTableEx::authors,
                                (q, t) -> q.orderBy(t.firstName())
                        )
                        .batchLoadCommand(
                                Arrays.asList(
                                        BookDraft.$.produce(book -> {
                                            book.setId(learningGraphQLId3);
                                        }),
                                        BookDraft.$.produce(book -> {
                                            book.setId(graphQLInActionId3);
                                        })
                                )
                        ),
                ctx -> {
                    ctx.sql(
                            "select " +
                                    "tb_1_.BOOK_ID, " +
                                    "tb_1_.AUTHOR_ID, tb_3_.FIRST_NAME, tb_3_.LAST_NAME, tb_3_.GENDER " +
                                    "from BOOK_AUTHOR_MAPPING as tb_1_ " +
                                    "inner join AUTHOR as tb_3_ on tb_1_.AUTHOR_ID = tb_3_.ID " +
                                    "where tb_1_.BOOK_ID in (?, ?) " +
                                    "order by tb_3_.FIRST_NAME asc"
                    );
                    ctx.variables(learningGraphQLId3, graphQLInActionId3);
                    ctx.rows(rows -> {
                        Assertions.assertEquals(1, rows.size());
                        String authorList1Text =
                                "[" +
                                        "--->{" +
                                        "--->--->\"id\":\"1e93da94-af84-44f4-82d1-d8a9fd52ea94\"," +
                                        "--->--->\"firstName\":\"Alex\"," +
                                        "--->--->\"lastName\":\"Banks\"," +
                                        "--->--->\"gender\":\"MALE\"" +
                                        "--->}, " +
                                        "--->{" +
                                        "--->--->\"id\":\"fd6bb6cf-336d-416c-8005-1ae11a6694b5\"," +
                                        "--->--->\"firstName\":\"Eve\"," +
                                        "--->--->\"lastName\":\"Procello\"," +
                                        "--->--->\"gender\":\"MALE\"" +
                                        "--->}" +
                                        "]";
                        String authorList2Text =
                                "[" +
                                        "--->{" +
                                        "--->--->\"id\":\"eb4963fd-5223-43e8-b06b-81e6172ee7ae\"," +
                                        "--->--->\"firstName\":\"Samer\"," +
                                        "--->--->\"lastName\":\"Buna\"," +
                                        "--->--->\"gender\":\"MALE\"" +
                                        "--->}" +
                                        "]";
                        Assertions.assertEquals(
                                authorList1Text.replace("--->", ""),
                                rows.get(0).get(
                                        BookDraft.$.produce(book -> {
                                            book.setId(learningGraphQLId3);
                                        })
                                ).toString()
                        );
                        Assertions.assertEquals(
                                authorList2Text.replace("--->", ""),
                                rows.get(0).get(
                                        BookDraft.$.produce(book -> {
                                            book.setId(graphQLInActionId3);
                                        })
                                ).toString()
                        );
                    });
                }
        );
    }
}
