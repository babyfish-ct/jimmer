package org.babyfish.jimmer.sql.oracle;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.common.NativeDatabases;
import org.babyfish.jimmer.sql.dialect.OracleDialect;
import org.babyfish.jimmer.sql.meta.UUIDIdGenerator;
import org.babyfish.jimmer.sql.model.*;
import org.babyfish.jimmer.sql.runtime.ScalarProvider;
import org.junit.jupiter.api.Test;

public class OracleQueryTest extends AbstractQueryTest {

    @Test
    public void testFetcher() {

        NativeDatabases.assumeOracleDatabase();

        BookStoreTable table = BookStoreTable.$;
        executeAndExpect(
                NativeDatabases.ORACLE_DATA_SOURCE,
                getSqlClient(cfg -> {
                    cfg
                            .setDialect(new OracleDialect())
                            .addScalarProvider(ScalarProvider.UUID_BY_STRING);
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
                                    "where tb_1_.ID in (?) group by tb_1_.ID"
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
                                    "--->--->\"avgPrice\":80.33333333333333333333333333333333333333" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testPage() {

        NativeDatabases.assumeOracleDatabase();

        BookTable table = BookTable.$;

        executeAndExpect(
                NativeDatabases.ORACLE_DATA_SOURCE,
                getSqlClient(cfg -> {
                    cfg
                            .setDialect(new OracleDialect())
                            .addScalarProvider(ScalarProvider.UUID_BY_STRING);
                })
                        .createQuery(table)
                        .orderBy(
                                table.name().asc(),
                                table.edition().desc()
                        )
                        .select(table)
                        .limit(5, 2),
                ctx -> {
                    ctx.sql(
                            "select * from (" +
                                    "--->select core__.*, rownum rn__ " +
                                    "--->from (" +
                                    "--->--->select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                    "--->--->from BOOK tb_1_ " +
                                    "--->--->order by tb_1_.NAME asc, tb_1_.EDITION desc" +
                                    "--->) core__ " +
                                    "--->where rownum <= ?" +
                                    ") limited__ where rn__ > ?"
                    ).variables(7, 2);
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":\"8f30bc8a-49f9-481d-beca-5fe2d147c831\"," +
                                    "--->--->\"name\":\"Effective TypeScript\"," +
                                    "--->--->\"edition\":1," +
                                    "--->--->\"price\":73," +
                                    "--->--->\"store\":{" +
                                    "--->--->--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"" +
                                    "--->--->}" +
                                    "--->}," +
                                    "--->{" +
                                    "--->--->\"id\":\"780bdf07-05af-48bf-9be9-f8c65236fecc\"," +
                                    "--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->\"edition\":3," +
                                    "--->--->\"price\":80," +
                                    "--->--->\"store\":{" +
                                    "--->--->--->\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"" +
                                    "--->--->}" +
                                    "--->}," +
                                    "--->{" +
                                    "--->--->\"id\":\"e37a8344-73bb-4b23-ba76-82eac11f03e6\"," +
                                    "--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->\"edition\":2," +
                                    "--->--->\"price\":81," +
                                    "--->--->\"store\":{" +
                                    "--->--->--->\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"" +
                                    "--->--->}" +
                                    "--->}," +
                                    "--->{" +
                                    "--->--->\"id\":\"a62f7aa3-9490-4612-98b5-98aae0e77120\"," +
                                    "--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->\"edition\":1," +
                                    "--->--->\"price\":80," +
                                    "--->--->\"store\":{" +
                                    "--->--->--->\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"" +
                                    "--->--->}" +
                                    "--->}," +
                                    "--->{" +
                                    "--->--->\"id\":\"64873631-5d82-4bae-8eb8-72dd955bfc56\"," +
                                    "--->--->\"name\":\"Learning GraphQL\"," +
                                    "--->--->\"edition\":3," +
                                    "--->--->\"price\":51," +
                                    "--->--->\"store\":{" +
                                    "--->--->--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"" +
                                    "--->--->}" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testPageWithOptimization() {

        NativeDatabases.assumeOracleDatabase();

        BookTable table = BookTable.$;

        executeAndExpect(
                NativeDatabases.ORACLE_DATA_SOURCE,
                getSqlClient(cfg -> {
                    cfg
                            .setDialect(new OracleDialect())
                            .addScalarProvider(ScalarProvider.UUID_BY_STRING)
                            .setOffsetOptimizingThreshold(2);
                })
                        .createQuery(table)
                        .orderBy(
                                table.name().asc(),
                                table.edition().desc()
                        )
                        .select(table)
                        .limit(5, 2),
                ctx -> {
                    ctx.sql(
                            "select optimize_.ID, optimize_.NAME, optimize_.EDITION, optimize_.PRICE, optimize_.STORE_ID from (" +
                                    "--->select limited__.*, rownum optimize_rn__ from (" +
                                    "--->--->select core__.*, rownum rn__ from (" +
                                    "--->--->--->select tb_1_.ID optimize_core_id_ " +
                                    "--->--->--->from BOOK tb_1_ " +
                                    "--->--->--->order by tb_1_.NAME asc, tb_1_.EDITION desc" +
                                    "--->--->) core__ " +
                                    "--->--->where rownum <= ?" +
                                    "--->) " +
                                    "--->limited__ where rn__ > ?" +
                                    ") optimize_core_ " +
                                    "inner join BOOK optimize_ " +
                                    "--->on optimize_.ID = optimize_core_.optimize_core_id_ " +
                                    "order by optimize_core_.optimize_rn__"
                    ).variables(7, 2);
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":\"8f30bc8a-49f9-481d-beca-5fe2d147c831\"," +
                                    "--->--->\"name\":\"Effective TypeScript\"," +
                                    "--->--->\"edition\":1," +
                                    "--->--->\"price\":73," +
                                    "--->--->\"store\":{" +
                                    "--->--->--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"" +
                                    "--->--->}" +
                                    "--->}," +
                                    "--->{" +
                                    "--->--->\"id\":\"780bdf07-05af-48bf-9be9-f8c65236fecc\"," +
                                    "--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->\"edition\":3," +
                                    "--->--->\"price\":80," +
                                    "--->--->\"store\":{" +
                                    "--->--->--->\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"" +
                                    "--->--->}" +
                                    "--->}," +
                                    "--->{" +
                                    "--->--->\"id\":\"e37a8344-73bb-4b23-ba76-82eac11f03e6\"," +
                                    "--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->\"edition\":2," +
                                    "--->--->\"price\":81," +
                                    "--->--->\"store\":{" +
                                    "--->--->--->\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"" +
                                    "--->--->}" +
                                    "--->}," +
                                    "--->{" +
                                    "--->--->\"id\":\"a62f7aa3-9490-4612-98b5-98aae0e77120\"," +
                                    "--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->\"edition\":1," +
                                    "--->--->\"price\":80," +
                                    "--->--->\"store\":{" +
                                    "--->--->--->\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"" +
                                    "--->--->}" +
                                    "--->}," +
                                    "--->{" +
                                    "--->--->\"id\":\"64873631-5d82-4bae-8eb8-72dd955bfc56\"," +
                                    "--->--->\"name\":\"Learning GraphQL\"," +
                                    "--->--->\"edition\":3," +
                                    "--->--->\"price\":51," +
                                    "--->--->\"store\":{" +
                                    "--->--->--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"" +
                                    "--->--->}" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testTuple() {

        NativeDatabases.assumeOracleDatabase();

        BookTable table = BookTable.$;
        executeAndExpect(
                NativeDatabases.ORACLE_DATA_SOURCE,
                getSqlClient(cfg -> {
                    cfg
                            .setDialect(new OracleDialect())
                            .addScalarProvider(ScalarProvider.UUID_BY_STRING);
                })
                        .createQuery(table)
                        .where(
                                Expression.tuple(table.name(), table.edition()).in(
                                        getSqlClient()
                                                .createSubQuery(table)
                                                .groupBy(table.name())
                                                .select(
                                                        table.name(),
                                                        table.edition().max()
                                                )
                                )
                        )
                        .orderBy(table.name())
                        .select(table),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                    "from BOOK tb_1_ " +
                                    "where (tb_1_.NAME, tb_1_.EDITION) in (" +
                                    "--->select tb_2_.NAME, max(tb_2_.EDITION) " +
                                    "--->from BOOK tb_2_ " +
                                    "--->group by tb_2_.NAME" +
                                    ") " +
                                    "order by tb_1_.NAME asc"
                    );
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":\"9eded40f-6d2e-41de-b4e7-33a28b11c8b6\"," +
                                    "--->--->\"name\":\"Effective TypeScript\"," +
                                    "--->--->\"edition\":3," +
                                    "--->--->\"price\":88," +
                                    "--->--->\"store\":{" +
                                    "--->--->--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"" +
                                    "--->--->}" +
                                    "--->}," +
                                    "--->{" +
                                    "--->--->\"id\":\"780bdf07-05af-48bf-9be9-f8c65236fecc\"," +
                                    "--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->\"edition\":3," +
                                    "--->--->\"price\":80," +
                                    "--->--->\"store\":{" +
                                    "--->--->--->\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"" +
                                    "--->--->}" +
                                    "--->}," +
                                    "--->{" +
                                    "--->--->\"id\":\"64873631-5d82-4bae-8eb8-72dd955bfc56\"," +
                                    "--->--->\"name\":\"Learning GraphQL\"," +
                                    "--->--->\"edition\":3," +
                                    "--->--->\"price\":51," +
                                    "--->--->\"store\":{" +
                                    "--->--->--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"" +
                                    "--->--->}" +
                                    "--->}," +
                                    "--->{" +
                                    "--->--->\"id\":\"782b9a9d-eac8-41c4-9f2d-74a5d047f45a\"," +
                                    "--->--->\"name\":\"Programming TypeScript\"," +
                                    "--->--->\"edition\":3," +
                                    "--->--->\"price\":48," +
                                    "--->--->\"store\":{" +
                                    "--->--->--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"" +
                                    "--->--->}" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }
}
