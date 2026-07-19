package org.babyfish.jimmer.sql.time;

import org.babyfish.jimmer.jackson.v2.JsonCodecV2;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.SqlTimeUnit;
import org.babyfish.jimmer.sql.ast.table.WeakJoin;
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

    private static String toString(Object o) {
        try {
            return new JsonCodecV2().writer().writeAsString(o);
        } catch (Exception e) {
            Assertions.fail();
            return "";
        }
    }

    @Test
    public void testMinusSecondsByH2() {
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

    @Test
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
                            "select " +
                                    "datetime(tb_1_.VALUE1, '-3 seconds'), " +
                                    "datetime(tb_1_.VALUE2, '-3 seconds'), " +
                                    "datetime(tb_1_.VALUE3, '-3 seconds'), " +
                                    "datetime(tb_1_.VALUE4, '-3 seconds'), " +
                                    "datetime(tb_1_.VALUE5, '-3 seconds'), " +
                                    "datetime(tb_1_.VALUE6, '-3 seconds'), " +
                                    "datetime(tb_1_.VALUE7, '-3 seconds'), " +
                                    "case " +
                                    "--->when substr(tb_1_.VALUE8, -6, 1) = '+' or " +
                                    "--->substr(tb_1_.VALUE8, -6, 1) = '-' " +
                                    "--->then datetime(" +
                                    "--->--->substr(tb_1_.VALUE8, 1, length(tb_1_.VALUE8) - 6), " +
                                    "--->--->'-3 seconds'" +
                                    "--->) || substr(tb_1_.VALUE8, -6, 6) " +
                                    "--->else datetime(tb_1_.VALUE8, '-3 seconds') " +
                                    "end, " +
                                    "case when " +
                                    "--->substr(tb_1_.VALUE9, -6, 1) = '+' or " +
                                    "--->substr(tb_1_.VALUE9, -6, 1) = '-' " +
                                    "--->then datetime(" +
                                    "--->--->substr(tb_1_.VALUE9, 1, length(tb_1_.VALUE9) - 6), " +
                                    "--->--->'-3 seconds'" +
                                    "--->) || substr(tb_1_.VALUE9, -6, 6) " +
                                    "--->else datetime(tb_1_.VALUE9, '-3 seconds') " +
                                    "end " +
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
    public void testMinusQuartersBySQLite() {
        DataSource dataSource = NativeDatabases.SQLITE_DATA_SOURCE;
        jdbc(dataSource, false, con -> initDatabase(con, "database-sqlite.sql"));

        TimeRowTable table = TimeRowTable.$;
        executeAndExpect(
                NativeDatabases.SQLITE_DATA_SOURCE,
                getSqlClient(it -> it.setDialect(new SQLiteDialect()))
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
                            "select datetime(tb_1_.VALUE1, '-6 months'), " +
                                    "datetime(tb_1_.VALUE2, '-6 months'), " +
                                    "datetime(tb_1_.VALUE4, '-6 months'), " +
                                    "datetime(tb_1_.VALUE5, '-6 months'), " +
                                    "datetime(tb_1_.VALUE7, '-6 months'), " +
                                    "case " +
                                    "--->when substr(tb_1_.VALUE8, -6, 1) = '+' or " +
                                    "--->substr(tb_1_.VALUE8, -6, 1) = '-' " +
                                    "--->then " +
                                    "--->--->datetime(" +
                                    "--->--->substr(tb_1_.VALUE8, 1, length(tb_1_.VALUE8) - 6), '-6 months') || " +
                                    "--->--->substr(tb_1_.VALUE8, -6, 6) " +
                                    "--->else " +
                                    "--->--->datetime(tb_1_.VALUE8, '-6 months') " +
                                    "end, " +
                                    "case " +
                                    "--->when substr(tb_1_.VALUE9, -6, 1) = '+' or substr(tb_1_.VALUE9, -6, 1) = '-' " +
                                    "--->--->then " +
                                    "--->--->--->datetime(substr(tb_1_.VALUE9, 1, length(tb_1_.VALUE9) - 6), '-6 months') || " +
                                    "--->--->--->substr(tb_1_.VALUE9, -6, 6) " +
                                    "--->--->else " +
                                    "--->--->--->datetime(tb_1_.VALUE9, '-6 months') " +
                                    "end " +
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

    @Test
    public void timeDiffByH2() {
        TimeRowTable table = TimeRowTable.$;
        TimeRowTable table2 = table.asTableEx().weakJoin(WeakJoinImpl.class);
        executeAndExpect(
                getSqlClient(it -> it.setDialect(new H2Dialect()))
                        .createQuery(table)
                        .where(table.id().eq(1L))
                        .select(
                                table2.value1().diff(table.value1(), SqlTimeUnit.HOURS),
                                table2.value4().diff(table.value4(), SqlTimeUnit.HOURS),
                                table2.value7().diff(table.value7(), SqlTimeUnit.HOURS),
                                table2.value8().diff(table.value8(), SqlTimeUnit.HOURS),
                                table2.value9().diff(table.value9(), SqlTimeUnit.HOURS)
                        ),
                ctx -> {
                    ctx.sql(
                            "select (tb_2_.VALUE1 - tb_1_.VALUE1) * 24, " +
                                    "(tb_2_.VALUE4 - tb_1_.VALUE4) * 24, " +
                                    "(tb_2_.VALUE7 - tb_1_.VALUE7) * 24, " +
                                    "(tb_2_.VALUE8 - tb_1_.VALUE8) * 24, " +
                                    "(tb_2_.VALUE9 - tb_1_.VALUE9) * 24 " +
                                    "from TIME_ROW tb_1_ " +
                                    "inner join TIME_ROW tb_2_ on tb_2_.ID = ? " +
                                    "where tb_1_.ID = ?"
                    );
                    ctx.rows("[{\"_1\":5.0055556,\"_2\":5.0055556,\"_3\":5.0055556,\"_4\":5.0055556,\"_5\":5.0055556}]");
                }
        );
    }

    @Test
    public void timeDiffBySQLite() {
        DataSource dataSource = NativeDatabases.SQLITE_DATA_SOURCE;
        jdbc(dataSource, false, con -> initDatabase(con, "database-sqlite.sql"));

        TimeRowTable table = TimeRowTable.$;
        TimeRowTable table2 = table.asTableEx().weakJoin(WeakJoinImpl.class);
        executeAndExpect(
                NativeDatabases.SQLITE_DATA_SOURCE,
                getSqlClient(it -> it.setDialect(new SQLiteDialect()))
                        .createQuery(table)
                        .where(table.id().eq(1L))
                        .select(
                                table2.value1().diff(table.value1(), SqlTimeUnit.HOURS),
                                table2.value4().diff(table.value4(), SqlTimeUnit.HOURS),
                                table2.value7().diff(table.value7(), SqlTimeUnit.HOURS),
                                table2.value8().diff(table.value8(), SqlTimeUnit.HOURS),
                                table2.value9().diff(table.value9(), SqlTimeUnit.HOURS)
                        ),
                ctx -> {
                    ctx.sql(
                            "select (julianday(tb_2_.VALUE1) - julianday(tb_1_.VALUE1)) * 24, (julianday(tb_2_.VALUE4) - julianday(tb_1_.VALUE4)) * 24, (julianday(tb_2_.VALUE7) - julianday(tb_1_.VALUE7)) * 24, (julianday(tb_2_.VALUE8) - julianday(tb_1_.VALUE8)) * 24, (julianday(tb_2_.VALUE9) - julianday(tb_1_.VALUE9)) * 24 from TIME_ROW tb_1_ inner join TIME_ROW tb_2_ on tb_2_.ID = ? where tb_1_.ID = ?"
                    );
                    ctx.rows("[{\"_1\":5.0055556,\"_2\":5.0055556,\"_3\":5.0055556,\"_4\":5.0055556,\"_5\":5.0055556}]");
                }
        );
    }

    @Test
    public void timeDiffByMySQL() {

        NativeDatabases.assumeNativeDatabase();

        TimeRowTable table = TimeRowTable.$;
        TimeRowTable table2 = table.asTableEx().weakJoin(WeakJoinImpl.class);
        executeAndExpect(
                NativeDatabases.MYSQL_DATA_SOURCE,
                getSqlClient(it -> it.setDialect(new MySqlDialect()))
                        .createQuery(table)
                        .where(table.id().eq(1L))
                        .select(
                                table2.value1().diff(table.value1(), SqlTimeUnit.HOURS),
                                table2.value4().diff(table.value4(), SqlTimeUnit.HOURS),
                                table2.value7().diff(table.value7(), SqlTimeUnit.HOURS),
                                table2.value8().diff(table.value8(), SqlTimeUnit.HOURS),
                                table2.value9().diff(table.value9(), SqlTimeUnit.HOURS)
                        ),
                ctx -> {
                    ctx.sql(
                            "select timestampdiff(microsecond, tb_1_.VALUE1, tb_2_.VALUE1) / 3600000000, " +
                                    "timestampdiff(microsecond, tb_1_.VALUE4, tb_2_.VALUE4) / 3600000000, " +
                                    "timestampdiff(microsecond, tb_1_.VALUE7, tb_2_.VALUE7) / 3600000000, " +
                                    "timestampdiff(microsecond, tb_1_.VALUE8, tb_2_.VALUE8) / 3600000000, " +
                                    "timestampdiff(microsecond, tb_1_.VALUE9, tb_2_.VALUE9) / 3600000000 " +
                                    "from TIME_ROW tb_1_ " +
                                    "inner join TIME_ROW tb_2_ on tb_2_.ID = ? " +
                                    "where tb_1_.ID = ?"
                    );
                    ctx.rows("[{\"_1\":5.0056,\"_2\":5.0056,\"_3\":5.0056,\"_4\":5.0056,\"_5\":5.0056}]");
                }
        );
    }

    @Test
    public void timeDiffByPostgres() {

        NativeDatabases.assumeNativeDatabase();

        TimeRowTable table = TimeRowTable.$;
        TimeRowTable table2 = table.asTableEx().weakJoin(WeakJoinImpl.class);
        executeAndExpect(
                NativeDatabases.POSTGRES_DATA_SOURCE,
                getSqlClient(it -> it.setDialect(new PostgresDialect()))
                        .createQuery(table)
                        .where(table.id().eq(1L))
                        .select(
                                table2.value1().diff(table.value1(), SqlTimeUnit.HOURS),
                                table2.value4().diff(table.value4(), SqlTimeUnit.HOURS),
                                table2.value7().diff(table.value7(), SqlTimeUnit.HOURS),
                                table2.value8().diff(table.value8(), SqlTimeUnit.HOURS),
                                table2.value9().diff(table.value9(), SqlTimeUnit.HOURS)
                        ),
                ctx -> {
                    ctx.sql(
                            "select extract(epoch from tb_2_.VALUE1 - tb_1_.VALUE1) / 3600, " +
                                    "extract(epoch from tb_2_.VALUE4 - tb_1_.VALUE4) / 3600, " +
                                    "extract(epoch from tb_2_.VALUE7 - tb_1_.VALUE7) / 3600, " +
                                    "extract(epoch from tb_2_.VALUE8 - tb_1_.VALUE8) / 3600, " +
                                    "extract(epoch from tb_2_.VALUE9 - tb_1_.VALUE9) / 3600 " +
                                    "from TIME_ROW tb_1_ inner join TIME_ROW tb_2_ on tb_2_.ID = ? " +
                                    "where tb_1_.ID = ?"
                    );
                    ctx.rows("[{\"_1\":5.0055556,\"_2\":5.0055556,\"_3\":5.0055556,\"_4\":5.0055556,\"_5\":5.0055556}]");
                }
        );
    }

    private static class WeakJoinImpl implements WeakJoin<TimeRowTable, TimeRowTable> {

        @Override
        public Predicate on(TimeRowTable source, TimeRowTable target) {
            return target.id().eq(2L);
        }
    }
}
