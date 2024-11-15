package org.babyfish.jimmer.sql.sqlite;

import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.common.NativeDatabases;
import org.babyfish.jimmer.sql.dialect.SQLiteDialect;
import org.babyfish.jimmer.sql.model.AuthorFetcher;
import org.babyfish.jimmer.sql.model.BookFetcher;
import org.babyfish.jimmer.sql.model.BookStoreFetcher;
import org.babyfish.jimmer.sql.model.BookStoreTable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;

public class SQLiteQueryTest extends AbstractQueryTest {

    @BeforeAll
    public static void beforeAll() {
        DataSource dataSource = NativeDatabases.SQLITE_DATA_SOURCE;
        jdbc(dataSource, false, con -> initDatabase(con, "database-sqlite.sql"));
    }

    @Test
    public void testFetcher() {
        BookStoreTable table = BookStoreTable.$;
        executeAndExpect(
                NativeDatabases.SQLITE_DATA_SOURCE,
                getSqlClient(cfg -> {
                    cfg.setDialect(new SQLiteDialect());
                })
                        .createQuery(table)
                        .where(table.name().eq("MANNING"))
                        .select(
                                table.fetch(
                                        BookStoreFetcher.$
                                                .allScalarFields()
                                                .avgPrice()
                                                .books(
                                                        BookFetcher.$
                                                                .allScalarFields()
                                                                .authors(
                                                                        AuthorFetcher.$
                                                                                .allScalarFields()
                                                                )
                                                )
                                )
                        ),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.WEBSITE, tb_1_.VERSION " +
                                    "from BOOK_STORE tb_1_ where tb_1_.NAME = ?"
                    );
                    ctx.statement(1).sql(
                            "select tb_1_.ID, coalesce(avg(tb_2_.PRICE), ?) " +
                                    "from BOOK_STORE tb_1_ " +
                                    "left join BOOK tb_2_ on tb_1_.ID = tb_2_.STORE_ID " +
                                    "where tb_1_.ID = ? group by tb_1_.ID"
                    );
                    ctx.statement(2).sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE " +
                                    "from BOOK tb_1_ " +
                                    "where tb_1_.STORE_ID = ?"
                    );
                    ctx.statement(3).sql(
                            "select tb_2_.BOOK_ID, tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME, tb_1_.GENDER " +
                                    "from AUTHOR tb_1_ " +
                                    "inner join BOOK_AUTHOR_MAPPING tb_2_ on tb_1_.ID = tb_2_.AUTHOR_ID " +
                                    "where tb_2_.BOOK_ID in (?, ?, ?)"
                    );
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"," +
                                    "--->--->\"name\":\"MANNING\"," +
                                    "--->--->\"website\":null," +
                                    "--->--->\"version\":0," +
                                    "--->--->\"books\":[" +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":\"a62f7aa3-9490-4612-98b5-98aae0e77120\"," +
                                    "--->--->--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->--->--->\"edition\":1," +
                                    "--->--->--->--->\"price\":80," +
                                    "--->--->--->--->\"authors\":[" +
                                    "--->--->--->--->--->{" +
                                    "--->--->--->--->--->--->\"id\":\"eb4963fd-5223-43e8-b06b-81e6172ee7ae\"," +
                                    "--->--->--->--->--->--->\"firstName\":\"Samer\"," +
                                    "--->--->--->--->--->--->\"lastName\":\"Buna\"," +
                                    "--->--->--->--->--->--->\"gender\":\"MALE\"" +
                                    "--->--->--->--->--->}" +
                                    "--->--->--->--->]" +
                                    "--->--->--->}," +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":\"e37a8344-73bb-4b23-ba76-82eac11f03e6\"," +
                                    "--->--->--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->--->--->\"edition\":2," +
                                    "--->--->--->--->\"price\":81," +
                                    "--->--->--->--->\"authors\":[" +
                                    "--->--->--->--->--->{" +
                                    "--->--->--->--->--->--->\"id\":\"eb4963fd-5223-43e8-b06b-81e6172ee7ae\"," +
                                    "--->--->--->--->--->--->\"firstName\":\"Samer\"," +
                                    "--->--->--->--->--->--->\"lastName\":\"Buna\"," +
                                    "--->--->--->--->--->--->\"gender\":\"MALE\"" +
                                    "--->--->--->--->--->}" +
                                    "--->--->--->--->]" +
                                    "--->--->--->}," +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":\"780bdf07-05af-48bf-9be9-f8c65236fecc\"," +
                                    "--->--->--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->--->--->\"edition\":3," +
                                    "--->--->--->--->\"price\":80," +
                                    "--->--->--->--->\"authors\":[" +
                                    "--->--->--->--->--->{" +
                                    "--->--->--->--->--->--->\"id\":\"eb4963fd-5223-43e8-b06b-81e6172ee7ae\"," +
                                    "--->--->--->--->--->--->\"firstName\":\"Samer\"," +
                                    "--->--->--->--->--->--->\"lastName\":\"Buna\"," +
                                    "--->--->--->--->--->--->\"gender\":\"MALE\"" +
                                    "--->--->--->--->--->}" +
                                    "--->--->--->--->]" +
                                    "--->--->--->}" +
                                    "--->--->]," +
                                    "--->--->\"avgPrice\":80.3333333333333" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }
}
