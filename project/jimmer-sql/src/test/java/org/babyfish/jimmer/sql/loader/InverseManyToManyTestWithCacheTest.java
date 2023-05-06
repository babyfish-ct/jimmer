package org.babyfish.jimmer.sql.loader;

import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.impl.DataLoader;
import org.babyfish.jimmer.sql.model.Author;
import org.babyfish.jimmer.sql.model.AuthorFetcher;
import org.babyfish.jimmer.sql.model.BookFetcher;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.junit.jupiter.api.Test;

import static org.babyfish.jimmer.sql.common.Constants.*;

public class InverseManyToManyTestWithCacheTest extends AbstractCachedLoaderTest {

    @Test
    public void loadTargetIds() {
        Fetcher<Author> fetcher = AuthorFetcher.$
                .books();
        for (int i = 0; i < 2; i++) {
            boolean useSql = i == 0;
            connectAndExpect(
                    con -> new DataLoader((JSqlClientImplementor)getCachedSqlClient(), con, fetcher.getFieldMap().get("books"))
                            .load(Entities.AUTHORS_FOR_MANY_TO_MANY),
                    ctx -> {
                        if (useSql) {
                            ctx.sql(
                                    "select tb_1_.AUTHOR_ID, tb_1_.BOOK_ID " +
                                            "from BOOK_AUTHOR_MAPPING tb_1_ " +
                                            "where tb_1_.AUTHOR_ID in (?, ?)"
                            ).variables(alexId, danId);
                        }
                        ctx.rows(1);
                        ctx.row(0, map -> {
                            expect(
                                    "[" +
                                            "--->{\"id\":\"e110c564-23cc-4811-9e81-d587a13db634\"}, " +
                                            "--->{\"id\":\"b649b11b-1161-4ad2-b261-af0112fdd7c8\"}, " +
                                            "--->{\"id\":\"64873631-5d82-4bae-8eb8-72dd955bfc56\"}" +
                                            "]",
                                    map.get(Entities.AUTHORS_FOR_MANY_TO_MANY.get(0))
                            );
                            expect(
                                    "[" +
                                            "--->{\"id\":\"8f30bc8a-49f9-481d-beca-5fe2d147c831\"}, " +
                                            "--->{\"id\":\"8e169cfb-2373-4e44-8cce-1f1277f730d1\"}, " +
                                            "--->{\"id\":\"9eded40f-6d2e-41de-b4e7-33a28b11c8b6\"}" +
                                            "]",
                                    map.get(Entities.AUTHORS_FOR_MANY_TO_MANY.get(1))
                            );
                        });
                    }
            );
        }
    }

    @Test
    public void loadTargetIdsWithFilter() {
        Fetcher<Author> fetcher = AuthorFetcher.$
                .books(
                        BookFetcher.$,
                        it -> it.filter(args ->
                                args.where(args.getTable().edition().eq(3))
                        )
                );
        for (int i = 0; i < 2; i++) {
            connectAndExpect(
                    con -> new DataLoader((JSqlClientImplementor) getCachedSqlClient(), con, fetcher.getFieldMap().get("books"))
                            .load(Entities.AUTHORS_FOR_MANY_TO_MANY),
                    ctx -> {
                        ctx.sql(
                                "select tb_2_.AUTHOR_ID, tb_1_.ID " +
                                        "from BOOK tb_1_ " +
                                        "inner join BOOK_AUTHOR_MAPPING tb_2_ on tb_1_.ID = tb_2_.BOOK_ID " +
                                        "where tb_2_.AUTHOR_ID in (?, ?) " +
                                        "and tb_1_.EDITION = ?"
                        ).variables(alexId, danId, 3);
                        ctx.rows(1);
                        ctx.row(0, map -> {
                            expect(
                                    "[" +
                                            "--->{\"id\":\"64873631-5d82-4bae-8eb8-72dd955bfc56\"}" +
                                            "]",
                                    map.get(Entities.AUTHORS_FOR_MANY_TO_MANY.get(0))
                            );
                            expect(
                                    "[" +
                                            "--->{\"id\":\"9eded40f-6d2e-41de-b4e7-33a28b11c8b6\"}" +
                                            "]",
                                    map.get(Entities.AUTHORS_FOR_MANY_TO_MANY.get(1))
                            );
                        });
                    }
            );
        }
    }

    @Test
    public void loadTargetDetails() {
        Fetcher<Author> fetcher = AuthorFetcher.$
                .books(
                        BookFetcher.$.name().edition()
                );
        for (int i = 0; i < 2; i++) {
            boolean useSql = i == 0;
            connectAndExpect(
                    con -> new DataLoader((JSqlClientImplementor)getCachedSqlClient(), con, fetcher.getFieldMap().get("books"))
                            .load(Entities.AUTHORS_FOR_MANY_TO_MANY),
                    ctx -> {
                        if (useSql) {
                            ctx.sql(
                                    "select tb_1_.AUTHOR_ID, tb_1_.BOOK_ID " +
                                            "from BOOK_AUTHOR_MAPPING tb_1_ " +
                                            "where tb_1_.AUTHOR_ID in (?, ?)"
                            ).variables(alexId, danId);
                            ctx.statement(1).sql(
                                    "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                            "from BOOK tb_1_ " +
                                            "where tb_1_.ID in (?, ?, ?, ?, ?, ?)"
                            ).variables(
                                    learningGraphQLId1, learningGraphQLId2, learningGraphQLId3,
                                    effectiveTypeScriptId1, effectiveTypeScriptId2, effectiveTypeScriptId3
                            );
                        }
                        ctx.rows(1);
                        ctx.row(0, map -> {
                            expect(
                                    "[" +
                                            "--->{" +
                                            "--->--->\"id\":\"e110c564-23cc-4811-9e81-d587a13db634\"," +
                                            "--->--->\"name\":\"Learning GraphQL\"," +
                                            "--->--->\"edition\":1" +
                                            "--->}, {" +
                                            "--->--->\"id\":\"b649b11b-1161-4ad2-b261-af0112fdd7c8\"," +
                                            "--->--->\"name\":\"Learning GraphQL\"," +
                                            "--->--->\"edition\":2" +
                                            "--->}, {" +
                                            "--->--->\"id\":\"64873631-5d82-4bae-8eb8-72dd955bfc56\"," +
                                            "--->--->\"name\":\"Learning GraphQL\"," +
                                            "--->--->\"edition\":3" +
                                            "--->}" +
                                            "]",
                                    map.get(Entities.AUTHORS_FOR_MANY_TO_MANY.get(0))
                            );
                            expect(
                                    "[" +
                                            "--->{" +
                                            "--->--->\"id\":\"8f30bc8a-49f9-481d-beca-5fe2d147c831\"," +
                                            "--->--->\"name\":\"Effective TypeScript\"," +
                                            "--->--->\"edition\":1" +
                                            "--->}, {" +
                                            "--->--->\"id\":\"8e169cfb-2373-4e44-8cce-1f1277f730d1\"," +
                                            "--->--->\"name\":\"Effective TypeScript\"," +
                                            "--->--->\"edition\":2" +
                                            "--->}, {" +
                                            "--->--->\"id\":\"9eded40f-6d2e-41de-b4e7-33a28b11c8b6\"," +
                                            "--->--->\"name\":\"Effective TypeScript\"," +
                                            "--->--->\"edition\":3" +
                                            "--->}" +
                                            "]",
                                    map.get(Entities.AUTHORS_FOR_MANY_TO_MANY.get(1))
                            );
                        });
                    }
            );
        }
    }

    @Test
    public void loadTargetDetailsWithFilter() {
        Fetcher<Author> fetcher = AuthorFetcher.$
                .books(
                        BookFetcher.$.name().edition(),
                        it -> it.filter(args ->
                                args.where(args.getTable().edition().eq(3))
                        )
                );
        connectAndExpect(
                con -> new DataLoader((JSqlClientImplementor)getCachedSqlClient(), con, fetcher.getFieldMap().get("books"))
                        .load(Entities.AUTHORS_FOR_MANY_TO_MANY),
                ctx -> {
                    ctx.sql(
                            "select tb_2_.AUTHOR_ID, tb_1_.ID, tb_1_.NAME, tb_1_.EDITION " +
                                    "from BOOK tb_1_ " +
                                    "inner join BOOK_AUTHOR_MAPPING tb_2_ on tb_1_.ID = tb_2_.BOOK_ID " +
                                    "where tb_2_.AUTHOR_ID in (?, ?) " +
                                    "and tb_1_.EDITION = ?"
                    ).variables(alexId, danId, 3);
                    ctx.rows(1);
                    ctx.row(0, map -> {
                        expect(
                                "[" +
                                        "{" +
                                        "--->--->\"id\":\"64873631-5d82-4bae-8eb8-72dd955bfc56\"," +
                                        "--->--->\"name\":\"Learning GraphQL\"," +
                                        "--->--->\"edition\":3" +
                                        "--->}" +
                                        "]",
                                map.get(Entities.AUTHORS_FOR_MANY_TO_MANY.get(0))
                        );
                        expect(
                                "[" +
                                        "{" +
                                        "--->--->\"id\":\"9eded40f-6d2e-41de-b4e7-33a28b11c8b6\"," +
                                        "--->--->\"name\":\"Effective TypeScript\"," +
                                        "--->--->\"edition\":3" +
                                        "--->}" +
                                        "]",
                                map.get(Entities.AUTHORS_FOR_MANY_TO_MANY.get(1))
                        );
                    });
                }
        );
    }
}
