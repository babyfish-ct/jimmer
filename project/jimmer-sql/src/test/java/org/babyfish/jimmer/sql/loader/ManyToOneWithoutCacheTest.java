package org.babyfish.jimmer.sql.loader;

import org.babyfish.jimmer.sql.ast.LikeMode;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.impl.DataLoader;
import org.babyfish.jimmer.sql.model.Book;
import org.babyfish.jimmer.sql.model.BookFetcher;
import org.babyfish.jimmer.sql.model.BookStoreFetcher;
import org.junit.jupiter.api.Test;

import static org.babyfish.jimmer.sql.common.Constants.*;

public class ManyToOneWithoutCacheTest extends AbstractQueryTest {
    
    @Test
    public void loadParentId() {
        Fetcher<Book> fetcher = BookFetcher.$.store();
        connectAndExpect(
            con -> new DataLoader(getSqlClient(), con, fetcher.getFieldMap().get("store"))
                    .load(Entities.BOOKS_FOR_MANY_TO_ONE),
            ctx -> {
                ctx.sql(
                        "select tb_1_.ID, tb_1_.STORE_ID " +
                                "from BOOK tb_1_ " +
                                "where tb_1_.ID in (?, ?) " +
                                "and tb_1_.STORE_ID is not null"
                ).variables(learningGraphQLId2, graphQLInActionId2);
                ctx.rows(1);
                ctx.row(0, map -> {
                    expect(
                            "{\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"}",
                            map.get(Entities.BOOKS_FOR_MANY_TO_ONE.get(0))
                    );
                    expect(
                            "{\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"}",
                            map.get(Entities.BOOKS_FOR_MANY_TO_ONE.get(1))
                    );
                    expect(
                            "{\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"}",
                            map.get(Entities.BOOKS_FOR_MANY_TO_ONE.get(2))
                    );
                    expect(
                            "{\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"}",
                            map.get(Entities.BOOKS_FOR_MANY_TO_ONE.get(3))
                    );
                });
            }
        );
    }
    
    @Test
    public void loadParentIdWithFilter() {
        Fetcher<Book> fetcher = BookFetcher.$.store(
                BookStoreFetcher.$,
                it -> it.filter(
                        args -> args
                                .where(args.getTable().name().like("M", LikeMode.START))
                )
        );
        connectAndExpect(
                con -> new DataLoader(getSqlClient(), con, fetcher.getFieldMap().get("store"))
                        .load(Entities.BOOKS_FOR_MANY_TO_ONE),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID from " +
                                    "BOOK_STORE tb_1_ " +
                                    "where tb_1_.ID in (?, ?) " +
                                    "and tb_1_.NAME like ?"
                    ).variables(oreillyId, manningId, "M%");
                    ctx.statement(1).sql(
                            "select tb_2_.ID, tb_1_.ID " +
                                    "from BOOK_STORE tb_1_ " +
                                    "inner join BOOK tb_2_ on tb_1_.ID = tb_2_.STORE_ID " +
                                    "where tb_2_.ID in (?, ?) " +
                                    "and tb_1_.NAME like ?"
                    ).variables(learningGraphQLId2, graphQLInActionId2, "M%");
                    ctx.rows(1);
                    ctx.row(0, map -> {
                        expect(
                                null,
                                map.get(Entities.BOOKS_FOR_MANY_TO_ONE.get(0))
                        );
                        expect(
                                null,
                                map.get(Entities.BOOKS_FOR_MANY_TO_ONE.get(1))
                        );
                        expect(
                                "{\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"}",
                                map.get(Entities.BOOKS_FOR_MANY_TO_ONE.get(2))
                        );
                        expect(
                                "{\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"}",
                                map.get(Entities.BOOKS_FOR_MANY_TO_ONE.get(3))
                        );
                    });
                }
        );
    }

    @Test
    public void loadParentDetail() {
        Fetcher<Book> fetcher = BookFetcher.$.store(
                BookStoreFetcher.$.name()
        );
        connectAndExpect(
                con -> new DataLoader(getSqlClient(), con, fetcher.getFieldMap().get("store"))
                        .load(Entities.BOOKS_FOR_MANY_TO_ONE),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME " +
                                    "from BOOK_STORE tb_1_ " +
                                    "where tb_1_.ID in (?, ?)"
                    ).variables(oreillyId, manningId);
                    ctx.statement(1).sql(
                            "select tb_2_.ID, tb_1_.ID, tb_1_.NAME " +
                                    "from BOOK_STORE tb_1_ " +
                                    "inner join BOOK tb_2_ on tb_1_.ID = tb_2_.STORE_ID " +
                                    "where tb_2_.ID in (?, ?)"
                    ).variables(learningGraphQLId2, graphQLInActionId2);
                    ctx.rows(1);
                    ctx.row(0, map -> {
                        expect(
                                "{" +
                                        "--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"," +
                                        "--->\"name\":\"O'REILLY\"" +
                                        "}",
                                map.get(Entities.BOOKS_FOR_MANY_TO_ONE.get(0))
                        );
                        expect(
                                "{" +
                                        "--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"," +
                                        "--->\"name\":\"O'REILLY\"" +
                                        "}",
                                map.get(Entities.BOOKS_FOR_MANY_TO_ONE.get(1))
                        );
                        expect(
                                "{" +
                                        "--->\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"," +
                                        "--->\"name\":\"MANNING\"" +
                                        "}",
                                map.get(Entities.BOOKS_FOR_MANY_TO_ONE.get(2))
                        );
                        expect(
                                "{" +
                                        "--->\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"," +
                                        "--->\"name\":\"MANNING\"" +
                                        "}",
                                map.get(Entities.BOOKS_FOR_MANY_TO_ONE.get(3))
                        );
                    });
                }
        );
    }

    @Test
    public void loadParentDetailWithFilter() {
        Fetcher<Book> fetcher = BookFetcher.$.store(
                BookStoreFetcher.$.name(),
                it -> it.filter(
                        args -> args
                                .where(args.getTable().name().like("M", LikeMode.START))
                )
        );
        connectAndExpect(
                con -> new DataLoader(getSqlClient(), con, fetcher.getFieldMap().get("store"))
                        .load(Entities.BOOKS_FOR_MANY_TO_ONE),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME " +
                                    "from BOOK_STORE tb_1_ " +
                                    "where tb_1_.ID in (?, ?) " +
                                    "and tb_1_.NAME like ?"
                    ).variables(oreillyId, manningId, "M%");
                    ctx.statement(1).sql(
                            "select tb_2_.ID, tb_1_.ID, tb_1_.NAME " +
                                    "from BOOK_STORE tb_1_ " +
                                    "inner join BOOK tb_2_ on tb_1_.ID = tb_2_.STORE_ID " +
                                    "where tb_2_.ID in (?, ?) " +
                                    "and tb_1_.NAME like ?"
                    ).variables(learningGraphQLId2, graphQLInActionId2, "M%");
                    ctx.rows(1);
                    ctx.row(0, map -> {
                        expect(
                                null,
                                map.get(Entities.BOOKS_FOR_MANY_TO_ONE.get(0))
                        );
                        expect(
                                null,
                                map.get(Entities.BOOKS_FOR_MANY_TO_ONE.get(1))
                        );
                        expect(
                                "{" +
                                        "--->\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"," +
                                        "--->\"name\":\"MANNING\"" +
                                        "}",
                                map.get(Entities.BOOKS_FOR_MANY_TO_ONE.get(2))
                        );
                        expect(
                                "{" +
                                        "--->\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"," +
                                        "--->\"name\":\"MANNING\"" +
                                        "}",
                                map.get(Entities.BOOKS_FOR_MANY_TO_ONE.get(3))
                        );
                    });
                }
        );
    }
}
