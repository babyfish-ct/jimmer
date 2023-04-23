package org.babyfish.jimmer.sql.query;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.AuthorFetcher;
import org.babyfish.jimmer.sql.model.Book;
import static org.babyfish.jimmer.sql.common.Constants.*;

import org.babyfish.jimmer.sql.model.BookFetcher;
import org.babyfish.jimmer.sql.model.BookStoreFetcher;
import org.babyfish.jimmer.sql.runtime.ConnectionManager;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.Arrays;
import java.util.function.Function;

public class EntityTest extends AbstractQueryTest {

    @Test
    public void testFindById() {
        connectAndExpect(
                con -> getSqlClient(con)
                        .getEntities()
                        .findById(Book.class, graphQLInActionId3),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                    "from BOOK tb_1_ " +
                                    "where tb_1_.ID = ?"
                    ).variables(graphQLInActionId3);
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":\"780bdf07-05af-48bf-9be9-f8c65236fecc\"," +
                                    "--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->\"edition\":3," +
                                    "--->--->\"price\":80.00," +
                                    "--->--->\"store\":{\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"}" +
                                    "--->}" +
                                    "]"
                    );
                });
    }

    @Test
    public void testFindByIds() {
        connectAndExpect(
                con -> getSqlClient(con)
                        .getEntities()
                        .findByIds(
                                Book.class,
                                Arrays.asList(
                                        graphQLInActionId3,
                                        effectiveTypeScriptId3
                                )
                        ),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                    "from BOOK tb_1_ " +
                                    "where tb_1_.ID in (?, ?)"
                    ).variables(graphQLInActionId3, effectiveTypeScriptId3);
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":\"780bdf07-05af-48bf-9be9-f8c65236fecc\"," +
                                    "--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->\"edition\":3," +
                                    "--->--->\"price\":80.00," +
                                    "--->--->\"store\":{\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"}" +
                                    "--->},{" +
                                    "--->--->\"id\":\"9eded40f-6d2e-41de-b4e7-33a28b11c8b6\"," +
                                    "--->--->\"name\":\"Effective TypeScript\"," +
                                    "--->--->\"edition\":3," +
                                    "--->--->\"price\":88.00," +
                                    "--->--->\"store\":{\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"}" +
                                    "--->}" +
                                    "]"
                    );
                });
    }

    @Test
    public void testFindMapByIds() {
        connectAndExpect(
                con -> getSqlClient(con)
                        .getEntities()
                        .findMapByIds(
                                Book.class,
                                Arrays.asList(
                                        graphQLInActionId3,
                                        effectiveTypeScriptId3
                                )
                        ),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                    "from BOOK tb_1_ " +
                                    "where tb_1_.ID in (?, ?)"
                    ).variables(graphQLInActionId3, effectiveTypeScriptId3);
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"780bdf07-05af-48bf-9be9-f8c65236fecc\":{" +
                                    "--->--->--->\"id\":\"780bdf07-05af-48bf-9be9-f8c65236fecc\"," +
                                    "--->--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->--->\"edition\":3," +
                                    "--->--->--->\"price\":80.00," +
                                    "--->--->--->\"store\":{\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"}" +
                                    "--->--->}," +
                                    "--->--->\"9eded40f-6d2e-41de-b4e7-33a28b11c8b6\":{" +
                                    "--->--->--->\"id\":\"9eded40f-6d2e-41de-b4e7-33a28b11c8b6\"," +
                                    "--->--->--->\"name\":\"Effective TypeScript\"," +
                                    "--->--->--->\"edition\":3," +
                                    "--->--->--->\"price\":88.00," +
                                    "--->--->--->\"store\":{\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"}" +
                                    "--->--->}" +
                                    "--->}" +
                                    "]"
                    );
                });
    }

    @Test
    public void testFindByFetcherId() {
        connectAndExpect(
                con -> getSqlClient(con)
                        .getEntities()
                        .findById(
                                BookFetcher.$.allScalarFields()
                                        .store(BookStoreFetcher.$.allScalarFields())
                                        .authors(AuthorFetcher.$.allScalarFields()),
                                graphQLInActionId3
                        ),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                    "from BOOK tb_1_ " +
                                    "where tb_1_.ID = ?"
                    ).variables(graphQLInActionId3);
                    ctx.statement(1).sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.WEBSITE, tb_1_.VERSION " +
                                    "from BOOK_STORE tb_1_ " +
                                    "where tb_1_.ID = ?"
                    ).variables(manningId);
                    ctx.statement(2).sql(
                            "select " +
                                    "tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME, tb_1_.GENDER " +
                                    "from AUTHOR tb_1_ " +
                                    "inner join BOOK_AUTHOR_MAPPING tb_2_ on tb_1_.ID = tb_2_.AUTHOR_ID " +
                                    "where tb_2_.BOOK_ID = ?"
                    ).variables(graphQLInActionId3);
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":\"780bdf07-05af-48bf-9be9-f8c65236fecc\"," +
                                    "--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->\"edition\":3," +
                                    "--->--->\"price\":80.00," +
                                    "--->--->\"store\":{" +
                                    "--->--->--->\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"," +
                                    "--->--->--->\"name\":\"MANNING\"," +
                                    "--->--->--->\"website\":null," +
                                    "--->--->--->\"version\":0" +
                                    "--->--->}," +
                                    "--->--->\"authors\":[" +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":\"eb4963fd-5223-43e8-b06b-81e6172ee7ae\"," +
                                    "--->--->--->--->\"firstName\":\"Samer\"," +
                                    "--->--->--->--->\"lastName\":\"Buna\"," +
                                    "--->--->--->--->\"gender\":\"MALE\"" +
                                    "--->--->--->}" +
                                    "--->--->]" +
                                    "--->}" +
                                    "]"
                    );
                });
    }

    @Test
    public void testFindByFetcherIds() {
        connectAndExpect(
                con -> getSqlClient(con)
                        .getEntities()
                        .findByIds(
                                BookFetcher.$.allScalarFields()
                                        .store(BookStoreFetcher.$.allScalarFields())
                                        .authors(AuthorFetcher.$.allScalarFields()),
                                Arrays.asList(graphQLInActionId3, effectiveTypeScriptId3)
                        ),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                    "from BOOK tb_1_ " +
                                    "where tb_1_.ID in (?, ?)"
                    ).variables(graphQLInActionId3, effectiveTypeScriptId3);
                    ctx.statement(1).sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.WEBSITE, tb_1_.VERSION " +
                                    "from BOOK_STORE tb_1_ " +
                                    "where tb_1_.ID in (?, ?)"
                    ).variables(manningId, oreillyId);
                    ctx.statement(2).sql(
                            "select " +
                                    "tb_2_.BOOK_ID, " +
                                    "tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME, tb_1_.GENDER " +
                                    "from AUTHOR tb_1_ " +
                                    "inner join BOOK_AUTHOR_MAPPING tb_2_ on tb_1_.ID = tb_2_.AUTHOR_ID " +
                                    "where tb_2_.BOOK_ID in (?, ?)"
                    ).variables(graphQLInActionId3, effectiveTypeScriptId3);
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":\"780bdf07-05af-48bf-9be9-f8c65236fecc\"," +
                                    "--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->\"edition\":3," +
                                    "--->--->\"price\":80.00," +
                                    "--->--->\"store\":{" +
                                    "--->--->--->\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"," +
                                    "--->--->--->\"name\":\"MANNING\"," +
                                    "--->--->--->\"website\":null," +
                                    "--->--->--->\"version\":0" +
                                    "--->--->}," +
                                    "--->--->\"authors\":[" +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":\"eb4963fd-5223-43e8-b06b-81e6172ee7ae\"," +
                                    "--->--->--->--->\"firstName\":\"Samer\"," +
                                    "--->--->--->--->\"lastName\":\"Buna\"," +
                                    "--->--->--->--->\"gender\":\"MALE\"" +
                                    "--->--->--->}" +
                                    "--->--->]" +
                                    "--->}," +
                                    "--->{" +
                                    "--->--->\"id\":\"9eded40f-6d2e-41de-b4e7-33a28b11c8b6\"," +
                                    "--->--->\"name\":\"Effective TypeScript\"," +
                                    "--->--->\"edition\":3," +
                                    "--->--->\"price\":88.00," +
                                    "--->--->\"store\":{" +
                                    "--->--->--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"," +
                                    "--->--->--->\"name\":\"O'REILLY\"," +
                                    "--->--->--->\"website\":null," +
                                    "--->--->--->\"version\":0" +
                                    "--->--->}," +
                                    "--->--->\"authors\":[" +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":\"c14665c8-c689-4ac7-b8cc-6f065b8d835d\"," +
                                    "--->--->--->--->\"firstName\":\"Dan\"," +
                                    "--->--->--->--->\"lastName\":\"Vanderkam\"," +
                                    "--->--->--->--->\"gender\":\"MALE\"" +
                                    "--->--->--->}" +
                                    "--->--->]" +
                                    "--->}" +
                                    "]"
                    );
                });
    }

    @Test
    public void testFindMapByFetcherIds() {
        connectAndExpect(
                con -> getSqlClient(con)
                        .getEntities()
                        .findMapByIds(
                                BookFetcher.$.allScalarFields()
                                        .store(BookStoreFetcher.$.allScalarFields())
                                        .authors(AuthorFetcher.$.allScalarFields()),
                                Arrays.asList(graphQLInActionId3, effectiveTypeScriptId3)
                        ),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                    "from BOOK tb_1_ " +
                                    "where tb_1_.ID in (?, ?)"
                    ).variables(graphQLInActionId3, effectiveTypeScriptId3);
                    ctx.statement(1).sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.WEBSITE, tb_1_.VERSION " +
                                    "from BOOK_STORE tb_1_ " +
                                    "where tb_1_.ID in (?, ?)"
                    ).variables(manningId, oreillyId);
                    ctx.statement(2).sql(
                            "select " +
                                    "tb_2_.BOOK_ID, " +
                                    "tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME, tb_1_.GENDER " +
                                    "from AUTHOR tb_1_ " +
                                    "inner join BOOK_AUTHOR_MAPPING tb_2_ on tb_1_.ID = tb_2_.AUTHOR_ID " +
                                    "where tb_2_.BOOK_ID in (?, ?)"
                    ).variables(graphQLInActionId3, effectiveTypeScriptId3);
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"780bdf07-05af-48bf-9be9-f8c65236fecc\":{" +
                                    "--->--->--->\"id\":\"780bdf07-05af-48bf-9be9-f8c65236fecc\"," +
                                    "--->--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->--->\"edition\":3," +
                                    "--->--->--->\"price\":80.00," +
                                    "--->--->--->\"store\":{" +
                                    "--->--->--->--->\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"," +
                                    "--->--->--->--->\"name\":\"MANNING\"," +
                                    "--->--->--->--->\"website\":null," +
                                    "--->--->--->--->\"version\":0" +
                                    "--->--->--->}," +
                                    "--->--->--->\"authors\":[" +
                                    "--->--->--->--->{" +
                                    "--->--->--->--->--->\"id\":\"eb4963fd-5223-43e8-b06b-81e6172ee7ae\"," +
                                    "--->--->--->--->--->\"firstName\":\"Samer\"," +
                                    "--->--->--->--->--->\"lastName\":\"Buna\"," +
                                    "--->--->--->--->--->\"gender\":\"MALE\"" +
                                    "--->--->--->--->}" +
                                    "--->--->--->]" +
                                    "--->--->}," +
                                    "--->--->\"9eded40f-6d2e-41de-b4e7-33a28b11c8b6\":{" +
                                    "--->--->--->\"id\":\"9eded40f-6d2e-41de-b4e7-33a28b11c8b6\"," +
                                    "--->--->--->\"name\":\"Effective TypeScript\"," +
                                    "--->--->--->\"edition\":3," +
                                    "--->--->--->\"price\":88.00," +
                                    "--->--->--->\"store\":{" +
                                    "--->--->--->--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"," +
                                    "--->--->--->--->\"name\":\"O'REILLY\"," +
                                    "--->--->--->--->\"website\":null," +
                                    "--->--->--->--->\"version\":0" +
                                    "--->--->--->}," +
                                    "--->--->--->\"authors\":[" +
                                    "--->--->--->--->{" +
                                    "--->--->--->--->--->\"id\":\"c14665c8-c689-4ac7-b8cc-6f065b8d835d\"," +
                                    "--->--->--->--->--->\"firstName\":\"Dan\"," +
                                    "--->--->--->--->--->\"lastName\":\"Vanderkam\"," +
                                    "--->--->--->--->--->\"gender\":\"MALE\"" +
                                    "--->--->--->--->}" +
                                    "--->--->--->]" +
                                    "--->--->}" +
                                    "--->}" +
                                    "]"
                    );
                });
    }

    private JSqlClient getSqlClient(Connection con) {
        return getSqlClient(builder -> {
            builder.setConnectionManager(new ConnectionManager() {
                @Override
                public <R> R execute(Function<Connection, R> block) {
                    return block.apply(con);
                }
            });
        });
    }
}
