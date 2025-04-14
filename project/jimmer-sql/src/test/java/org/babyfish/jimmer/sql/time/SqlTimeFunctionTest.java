package org.babyfish.jimmer.sql.time;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.babyfish.jimmer.jackson.ImmutableModule;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.SqlTimeUnit;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.common.NativeDatabases;
import org.babyfish.jimmer.sql.dialect.H2Dialect;
import org.babyfish.jimmer.sql.dialect.MySqlDialect;
import org.babyfish.jimmer.sql.dialect.PostgresDialect;
import org.babyfish.jimmer.sql.dialect.SQLiteDialect;
import org.babyfish.jimmer.sql.model.time.TimeRowTable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;

public class SqlTimeFunctionTest extends AbstractQueryTest {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new ImmutableModule())
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private static String toString(Object o) {
        try {
            return MAPPER.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            Assertions.fail();
            return "";
        }
    }

    @Test
    public void testMinusSecondByH2() {
        TimeRowTable table = TimeRowTable.$;
        executeAndExpect(
                getSqlClient(it -> it.setDialect(new H2Dialect()))
                        .createQuery(table)
                        .where(table.id().eq(1L))
                        .select(
                                table.value1().minus(3, SqlTimeUnit.SECONDS),
                                table.value2().minus(3, SqlTimeUnit.SECONDS),
                                table.value3().minus(3, SqlTimeUnit.SECONDS),
                                table.value4().minus(3, SqlTimeUnit.SECONDS),
                                table.value5().minus(3, SqlTimeUnit.SECONDS),
                                table.value6().minus(3, SqlTimeUnit.SECONDS),
                                table.value7().minus(3, SqlTimeUnit.SECONDS),
                                table.value8().minus(3, SqlTimeUnit.SECONDS),
                                table.value9().minus(3, SqlTimeUnit.SECONDS)
                        ),
                ctx -> {
                    ctx.sql(
                            "select dateadd(second, ?, tb_1_.VALUE1), " +
                                    "dateadd(second, ?, tb_1_.VALUE2), " +
                                    "dateadd(second, ?, tb_1_.VALUE3), " +
                                    "dateadd(second, ?, tb_1_.VALUE4), " +
                                    "dateadd(second, ?, tb_1_.VALUE5), " +
                                    "dateadd(second, ?, tb_1_.VALUE6), " +
                                    "dateadd(second, ?, tb_1_.VALUE7), " +
                                    "dateadd(second, ?, tb_1_.VALUE8), " +
                                    "dateadd(second, ?, tb_1_.VALUE9) " +
                                    "from TIME_ROW tb_1_ where tb_1_.ID = ?"
                    );
                    ctx.row(0, row -> {
                        assertContentEquals(
                                "{\"_1\":\"2025-04-13T05:31:58.000+00:00\"," +
                                        "\"_2\":\"2025-04-12\"," +
                                        "\"_3\":\"13:32:00\"," +
                                        "\"_4\":\"2025-04-13T05:32:01.000+00:00\"," +
                                        "\"_5\":\"2025-04-12\"," +
                                        "\"_6\":\"13:32:03\"," +
                                        "\"_7\":\"2025-04-13T13:32:04\"," +
                                        "\"_8\":\"2025-04-13T13:32:05+08:00\"," +
                                        "\"_9\":\"2025-04-13T13:32:06+08:00\"}",
                                toString(row)
                        );
                    });
                }
        );
    }

    @Test
    public void testMinusQuartersByH2() {
        TimeRowTable table = TimeRowTable.$;
        executeAndExpect(
                getSqlClient(it -> it.setDialect(new H2Dialect()))
                        .createQuery(table)
                        .where(table.id().eq(1L))
                        .select(
                                table.value1().minus(2, SqlTimeUnit.QUARTERS),
                                table.value2().minus(2, SqlTimeUnit.QUARTERS),
                                table.value4().minus(2, SqlTimeUnit.QUARTERS),
                                table.value5().minus(2, SqlTimeUnit.QUARTERS),
                                table.value7().minus(2, SqlTimeUnit.QUARTERS),
                                table.value8().minus(2, SqlTimeUnit.QUARTERS),
                                table.value9().minus(2, SqlTimeUnit.QUARTERS)
                        ),
                ctx -> {
                    ctx.sql(
                            "select dateadd(month, ? * 3, tb_1_.VALUE1), dateadd(month, ? * 3, tb_1_.VALUE2), dateadd(month, ? * 3, tb_1_.VALUE4), dateadd(month, ? * 3, tb_1_.VALUE5), dateadd(month, ? * 3, tb_1_.VALUE7), dateadd(month, ? * 3, tb_1_.VALUE8), dateadd(month, ? * 3, tb_1_.VALUE9) from TIME_ROW tb_1_ where tb_1_.ID = ?"
                    );
                    ctx.row(0, row -> {
                        assertContentEquals(
                                "{\"_1\":\"2024-10-13T05:32:01.000+00:00\"," +
                                        "\"_2\":\"2024-10-13\"," +
                                        "\"_3\":\"2024-10-13T05:32:04.000+00:00\"," +
                                        "\"_4\":\"2024-10-13\"," +
                                        "\"_5\":\"2024-10-13T13:32:07\"," +
                                        "\"_6\":\"2024-10-13T13:32:08+08:00\"," +
                                        "\"_7\":\"2024-10-13T13:32:09+08:00\"}",
                                toString(row)
                        );
                    });
                }
        );
    }

    //@Test
    public void testMinusSecondBySQLite() {
        DataSource dataSource = NativeDatabases.SQLITE_DATA_SOURCE;
        jdbc(dataSource, false, con -> initDatabase(con, "database-sqlite.sql"));

        TimeRowTable table = TimeRowTable.$;
        executeAndExpect(
                NativeDatabases.SQLITE_DATA_SOURCE,
                getSqlClient(it -> it.setDialect(new SQLiteDialect()))
                        .createQuery(table)
                        .where(table.id().eq(1L))
                        .select(
                                table.value1().minus(3, SqlTimeUnit.SECONDS),
                                table.value2().minus(3, SqlTimeUnit.SECONDS),
                                table.value3().minus(3, SqlTimeUnit.SECONDS),
                                table.value4().minus(3, SqlTimeUnit.SECONDS),
                                table.value5().minus(3, SqlTimeUnit.SECONDS),
                                table.value6().minus(3, SqlTimeUnit.SECONDS),
                                table.value7().minus(3, SqlTimeUnit.SECONDS),
                                table.value8().minus(3, SqlTimeUnit.SECONDS),
                                table.value9().minus(3, SqlTimeUnit.SECONDS)
                        ),
                ctx -> {
                    ctx.sql(
                            "select datetime(tb_1_.VALUE1, '-3 seconds'), datetime(tb_1_.VALUE2, '-3 seconds'), datetime(tb_1_.VALUE3, '-3 seconds'), datetime(tb_1_.VALUE4, '-3 seconds'), datetime(tb_1_.VALUE5, '-3 seconds'), datetime(tb_1_.VALUE6, '-3 seconds'), datetime(tb_1_.VALUE7, '-3 seconds'), datetime(tb_1_.VALUE8, '-3 seconds'), datetime(tb_1_.VALUE9, '-3 seconds') from TIME_ROW tb_1_ where tb_1_.ID = ?"
                    );
                    ctx.row(0, row -> {
                        assertContentEquals(
                                "{\"_1\":\"2025-04-13T05:31:58.000+00:00\"," +
                                        "\"_2\":\"2025-04-12\"," +
                                        "\"_3\":\"13:32:00\"," +
                                        "\"_4\":\"2025-04-13T05:32:01.000+00:00\"," +
                                        "\"_5\":\"2025-04-12\"," +
                                        "\"_6\":\"13:32:03\"," +
                                        "\"_7\":\"2025-04-13T13:32:04\"," +
                                        "\"_8\":\"2025-04-13T13:32:05+08:00\"," +
                                        "\"_9\":\"2025-04-13T13:32:06+08:00\"}",
                                toString(row)
                        );
                    });
                }
        );
    }

    @Test
    public void testMinusSecondByMySQL() {

        NativeDatabases.assumeNativeDatabase();

        TimeRowTable table = TimeRowTable.$;
        executeAndExpect(
                NativeDatabases.MYSQL_DATA_SOURCE,
                getSqlClient(it -> it.setDialect(new MySqlDialect()))
                        .createQuery(table)
                        .where(table.id().eq(1L))
                        .select(
                                table.value1().minus(3, SqlTimeUnit.SECONDS),
                                table.value2().minus(3, SqlTimeUnit.SECONDS),
                                table.value3().minus(3, SqlTimeUnit.SECONDS),
                                table.value4().minus(3, SqlTimeUnit.SECONDS),
                                table.value5().minus(3, SqlTimeUnit.SECONDS),
                                table.value6().minus(3, SqlTimeUnit.SECONDS),
                                table.value7().minus(3, SqlTimeUnit.SECONDS),
                                table.value8().minus(3, SqlTimeUnit.SECONDS),
                                table.value9().minus(3, SqlTimeUnit.SECONDS)
                        ),
                ctx -> {
                    ctx.sql(
                            "select date_add(tb_1_.VALUE1, interval ? second), " +
                                    "date_add(tb_1_.VALUE2, interval ? second), " +
                                    "date_add(tb_1_.VALUE3, interval ? second), " +
                                    "date_add(tb_1_.VALUE4, interval ? second), " +
                                    "date_add(tb_1_.VALUE5, interval ? second), " +
                                    "date_add(tb_1_.VALUE6, interval ? second), " +
                                    "date_add(tb_1_.VALUE7, interval ? second), " +
                                    "date_add(tb_1_.VALUE8, interval ? second), " +
                                    "date_add(tb_1_.VALUE9, interval ? second) " +
                                    "from TIME_ROW tb_1_ where tb_1_.ID = ?"
                    );
                    ctx.row(0, row -> {
                        assertContentEquals(
                                "{\"_1\":\"2025-04-13T05:31:58.000+00:00\"," +
                                        "\"_2\":\"2025-04-12\"," +
                                        "\"_3\":\"13:32:00\"," +
                                        "\"_4\":\"2025-04-13T05:32:01.000+00:00\"," +
                                        "\"_5\":\"2025-04-12\"," +
                                        "\"_6\":\"13:32:03\"," +
                                        "\"_7\":\"2025-04-13T13:32:04\"," +
                                        "\"_8\":\"2025-04-13T13:32:05+08:00\"," +
                                        "\"_9\":\"2025-04-13T13:32:06+08:00\"}",
                                toString(row)
                        );
                    });
                }
        );
    }

    @Test
    public void testMinusQuartersByMySQL() {
        NativeDatabases.assumeNativeDatabase();
        TimeRowTable table = TimeRowTable.$;
        executeAndExpect(
                NativeDatabases.MYSQL_DATA_SOURCE,
                getSqlClient(it -> it.setDialect(new MySqlDialect()))
                        .createQuery(table)
                        .where(table.id().eq(1L))
                        .select(
                                table.value1().minus(2, SqlTimeUnit.QUARTERS),
                                table.value2().minus(2, SqlTimeUnit.QUARTERS),
                                table.value4().minus(2, SqlTimeUnit.QUARTERS),
                                table.value5().minus(2, SqlTimeUnit.QUARTERS),
                                table.value7().minus(2, SqlTimeUnit.QUARTERS),
                                table.value8().minus(2, SqlTimeUnit.QUARTERS),
                                table.value9().minus(2, SqlTimeUnit.QUARTERS)
                        ),
                ctx -> {
                    ctx.sql(
                            "select date_add(tb_1_.VALUE1, interval ? * 3 month), " +
                                    "date_add(tb_1_.VALUE2, interval ? * 3 month), " +
                                    "date_add(tb_1_.VALUE4, interval ? * 3 month), " +
                                    "date_add(tb_1_.VALUE5, interval ? * 3 month), " +
                                    "date_add(tb_1_.VALUE7, interval ? * 3 month), " +
                                    "date_add(tb_1_.VALUE8, interval ? * 3 month), " +
                                    "date_add(tb_1_.VALUE9, interval ? * 3 month) " +
                                    "from TIME_ROW tb_1_ where tb_1_.ID = ?"
                    );
                    ctx.row(0, row -> {
                        assertContentEquals(
                                "{\"_1\":\"2024-10-13T05:32:01.000+00:00\"," +
                                        "\"_2\":\"2024-10-13\"," +
                                        "\"_3\":\"2024-10-13T05:32:04.000+00:00\"," +
                                        "\"_4\":\"2024-10-13\"," +
                                        "\"_5\":\"2024-10-13T13:32:07\"," +
                                        "\"_6\":\"2024-10-13T13:32:08+08:00\"," +
                                        "\"_7\":\"2024-10-13T13:32:09+08:00\"}",
                                toString(row)
                        );
                    });
                }
        );
    }

    @Test
    public void testMinusSecondByPostgres() {

        NativeDatabases.assumeNativeDatabase();

        TimeRowTable table = TimeRowTable.$;
        executeAndExpect(
                NativeDatabases.POSTGRES_DATA_SOURCE,
                getSqlClient(it -> it.setDialect(new PostgresDialect()))
                        .createQuery(table)
                        .where(table.id().eq(1L))
                        .select(
                                table.value1().minus(3, SqlTimeUnit.SECONDS),
                                table.value2().minus(3, SqlTimeUnit.SECONDS),
                                table.value3().minus(3, SqlTimeUnit.SECONDS),
                                table.value4().minus(3, SqlTimeUnit.SECONDS),
                                table.value5().minus(3, SqlTimeUnit.SECONDS),
                                table.value6().minus(3, SqlTimeUnit.SECONDS),
                                table.value7().minus(3, SqlTimeUnit.SECONDS),
                                table.value8().minus(3, SqlTimeUnit.SECONDS),
                                table.value9().minus(3, SqlTimeUnit.SECONDS)
                        ),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.VALUE1 + ? * interval '1 second', " +
                                    "tb_1_.VALUE2 + ? * interval '1 second', " +
                                    "tb_1_.VALUE3 + ? * interval '1 second', " +
                                    "tb_1_.VALUE4 + ? * interval '1 second', " +
                                    "tb_1_.VALUE5 + ? * interval '1 second', " +
                                    "tb_1_.VALUE6 + ? * interval '1 second', " +
                                    "tb_1_.VALUE7 + ? * interval '1 second', " +
                                    "tb_1_.VALUE8 + ? * interval '1 second', " +
                                    "tb_1_.VALUE9 + ? * interval '1 second' from TIME_ROW tb_1_ where tb_1_.ID = ?"
                    );
                    ctx.row(0, row -> {
                        assertContentEquals(
                                "{\"_1\":\"2025-04-13T05:31:58.000+00:00\"," +
                                        "\"_2\":\"2025-04-12\"," +
                                        "\"_3\":\"13:32:00\"," +
                                        "\"_4\":\"2025-04-13T05:32:01.000+00:00\"," +
                                        "\"_5\":\"2025-04-12\"," +
                                        "\"_6\":\"13:32:03\"," +
                                        "\"_7\":\"2025-04-13T13:32:04\"," +
                                        "\"_8\":\"2025-04-13T13:32:05+08:00\"," +
                                        "\"_9\":\"2025-04-13T13:32:06+08:00\"}",
                                toString(row)
                        );
                    });
                }
        );
    }

    @Test
    public void testMinusQuartersByPostgres() {
        NativeDatabases.assumeNativeDatabase();
        TimeRowTable table = TimeRowTable.$;
        executeAndExpect(
                NativeDatabases.POSTGRES_DATA_SOURCE,
                getSqlClient(it -> it.setDialect(new PostgresDialect()))
                        .createQuery(table)
                        .where(table.id().eq(1L))
                        .select(
                                table.value1().minus(2, SqlTimeUnit.QUARTERS),
                                table.value2().minus(2, SqlTimeUnit.QUARTERS),
                                table.value4().minus(2, SqlTimeUnit.QUARTERS),
                                table.value5().minus(2, SqlTimeUnit.QUARTERS),
                                table.value7().minus(2, SqlTimeUnit.QUARTERS),
                                table.value8().minus(2, SqlTimeUnit.QUARTERS),
                                table.value9().minus(2, SqlTimeUnit.QUARTERS)
                        ),
                ctx -> {
                    ctx.sql(
                            "select " +
                                    "tb_1_.VALUE1 + ? * interval '3 month', " +
                                    "tb_1_.VALUE2 + ? * interval '3 month', " +
                                    "tb_1_.VALUE4 + ? * interval '3 month', " +
                                    "tb_1_.VALUE5 + ? * interval '3 month', " +
                                    "tb_1_.VALUE7 + ? * interval '3 month', " +
                                    "tb_1_.VALUE8 + ? * interval '3 month', " +
                                    "tb_1_.VALUE9 + ? * interval '3 month' " +
                                    "from TIME_ROW tb_1_ where tb_1_.ID = ?"
                    );
                    ctx.row(0, row -> {
                        assertContentEquals(
                                "{\"_1\":\"2024-10-13T05:32:01.000+00:00\"," +
                                        "\"_2\":\"2024-10-13\"," +
                                        "\"_3\":\"2024-10-13T05:32:04.000+00:00\"," +
                                        "\"_4\":\"2024-10-13\"," +
                                        "\"_5\":\"2024-10-13T13:32:07\"," +
                                        "\"_6\":\"2024-10-13T13:32:08+08:00\"," +
                                        "\"_7\":\"2024-10-13T13:32:09+08:00\"}",
                                toString(row)
                        );
                    });
                }
        );
    }
}
