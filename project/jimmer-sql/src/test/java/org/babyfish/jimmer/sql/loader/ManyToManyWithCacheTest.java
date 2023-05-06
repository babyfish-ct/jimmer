package org.babyfish.jimmer.sql.loader;

import org.babyfish.jimmer.sql.ast.LikeMode;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.impl.DataLoader;
import org.babyfish.jimmer.sql.model.AuthorFetcher;
import org.babyfish.jimmer.sql.model.Book;
import org.babyfish.jimmer.sql.model.BookFetcher;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.junit.jupiter.api.Test;

import static org.babyfish.jimmer.sql.common.Constants.*;

public class ManyToManyWithCacheTest extends AbstractCachedLoaderTest {

    @Test
    public void loadTargetIds() {
        Fetcher<Book> fetcher = BookFetcher.$.authors();
        for (int i = 0; i < 2; i++) {
            boolean useSql = i == 0;
            connectAndExpect(
                    con -> new DataLoader((JSqlClientImplementor) getCachedSqlClient(), con, fetcher.getFieldMap().get("authors"))
                            .load(Entities.BOOKS_FOR_MANY_TO_MANY),
                    ctx -> {
                        if (useSql) {
                            ctx.sql(
                                    "select tb_1_.BOOK_ID, tb_1_.AUTHOR_ID " +
                                            "from BOOK_AUTHOR_MAPPING tb_1_ " +
                                            "where tb_1_.BOOK_ID in (?, ?)"
                            ).variables(learningGraphQLId3, graphQLInActionId3);
                        }
                        ctx.rows(1);
                        ctx.row(0, map -> {
                            expect(
                                    "[" +
                                            "--->{\"id\":\"1e93da94-af84-44f4-82d1-d8a9fd52ea94\"}, " +
                                            "--->{\"id\":\"fd6bb6cf-336d-416c-8005-1ae11a6694b5\"}" +
                                            "]",
                                    map.get(Entities.BOOKS_FOR_MANY_TO_MANY.get(0))
                            );
                            expect(
                                    "[{\"id\":\"eb4963fd-5223-43e8-b06b-81e6172ee7ae\"}]",
                                    map.get(Entities.BOOKS_FOR_MANY_TO_MANY.get(1))
                            );
                        });
                    }
            );
        }
    }

    @Test
    public void loadTargetIdsWithFilter() {
        Fetcher<Book> fetcher = BookFetcher.$.authors(
                AuthorFetcher.$,
                it -> it.filter(args ->
                        args.where(args.getTable().firstName().like("A", LikeMode.START))
                )
        );
        for (int i = 0; i < 2; i++) {
            connectAndExpect(
                    con -> new DataLoader((JSqlClientImplementor) getCachedSqlClient(), con, fetcher.getFieldMap().get("authors"))
                            .load(Entities.BOOKS_FOR_MANY_TO_MANY),
                    ctx -> {
                        ctx.sql(
                                "select tb_2_.BOOK_ID, tb_1_.ID " +
                                        "from AUTHOR tb_1_ " +
                                        "inner join BOOK_AUTHOR_MAPPING tb_2_ on tb_1_.ID = tb_2_.AUTHOR_ID " +
                                        "where tb_2_.BOOK_ID in (?, ?) " +
                                        "and tb_1_.FIRST_NAME like ?"
                        ).variables(learningGraphQLId3, graphQLInActionId3, "A%");
                        ctx.rows(1);
                        ctx.row(0, map -> {
                            expect(
                                    "[{\"id\":\"1e93da94-af84-44f4-82d1-d8a9fd52ea94\"}]",
                                    map.get(Entities.BOOKS_FOR_MANY_TO_MANY.get(0))
                            );
                            expect(
                                    null,
                                    map.get(Entities.BOOKS_FOR_MANY_TO_MANY.get(1))
                            );
                        });
                    }
            );
        }
    }

    @Test
    public void loadTargetDetails() {
        Fetcher<Book> fetcher = BookFetcher.$.authors(
                AuthorFetcher.$.firstName().lastName()
        );
        connectAndExpect(
                con -> new DataLoader((JSqlClientImplementor) getCachedSqlClient(), con, fetcher.getFieldMap().get("authors"))
                        .load(Entities.BOOKS_FOR_MANY_TO_MANY),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.BOOK_ID, tb_1_.AUTHOR_ID " +
                                    "from BOOK_AUTHOR_MAPPING tb_1_ " +
                                    "where tb_1_.BOOK_ID in (?, ?)"
                    ).variables(learningGraphQLId3, graphQLInActionId3);
                    ctx.statement(1).sql(
                            "select tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME, tb_1_.GENDER " +
                                    "from AUTHOR tb_1_ " +
                                    "where tb_1_.ID in (?, ?, ?)"
                    ).variables(alexId, eveId, sammerId);
                    ctx.rows(1);
                    ctx.row(0, map -> {
                        expect(
                                "[" +
                                        "--->{" +
                                        "--->--->\"id\":\"1e93da94-af84-44f4-82d1-d8a9fd52ea94\"," +
                                        "--->--->\"firstName\":\"Alex\"," +
                                        "--->--->\"lastName\":\"Banks\"" +
                                        "--->}, {" +
                                        "--->--->\"id\":\"fd6bb6cf-336d-416c-8005-1ae11a6694b5\"," +
                                        "--->--->\"firstName\":\"Eve\"," +
                                        "--->--->\"lastName\":\"Procello\"" +
                                        "--->}" +
                                        "]",
                                map.get(Entities.BOOKS_FOR_MANY_TO_MANY.get(0))
                        );
                        expect(
                                "[" +
                                        "--->{" +
                                        "--->--->\"id\":\"eb4963fd-5223-43e8-b06b-81e6172ee7ae\"," +
                                        "--->--->\"firstName\":\"Samer\"," +
                                        "--->--->\"lastName\":\"Buna\"" +
                                        "--->}" +
                                        "]",
                                map.get(Entities.BOOKS_FOR_MANY_TO_MANY.get(1))
                        );
                    });
                }
        );
    }

    @Test
    public void loadTargetDetailsWithFilter() {
        Fetcher<Book> fetcher = BookFetcher.$.authors(
                AuthorFetcher.$.firstName().lastName(),
                it -> it.filter(args ->
                        args.where(args.getTable().firstName().like("A", LikeMode.START))
                )
        );
        connectAndExpect(
                con -> new DataLoader((JSqlClientImplementor) getCachedSqlClient(), con, fetcher.getFieldMap().get("authors"))
                        .load(Entities.BOOKS_FOR_MANY_TO_MANY),
                ctx -> {
                    ctx.sql(
                            "select tb_2_.BOOK_ID, tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME " +
                                    "from AUTHOR tb_1_ " +
                                    "inner join BOOK_AUTHOR_MAPPING tb_2_ on tb_1_.ID = tb_2_.AUTHOR_ID " +
                                    "where tb_2_.BOOK_ID in (?, ?) " +
                                    "and tb_1_.FIRST_NAME like ?"
                    ).variables(learningGraphQLId3, graphQLInActionId3, "A%");
                    ctx.rows(1);
                    ctx.row(0, map -> {
                        expect(
                                "[" +
                                        "--->{" +
                                        "--->--->\"id\":\"1e93da94-af84-44f4-82d1-d8a9fd52ea94\"," +
                                        "--->--->\"firstName\":\"Alex\"," +
                                        "--->--->\"lastName\":\"Banks\"" +
                                        "--->}" +
                                        "]",
                                map.get(Entities.BOOKS_FOR_MANY_TO_MANY.get(0))
                        );
                        expect(
                                null,
                                map.get(Entities.BOOKS_FOR_MANY_TO_MANY.get(1))
                        );
                    });
                }
        );
    }
}
