package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.collection.TypedList;
import org.babyfish.jimmer.sql.common.NativeDatabases;
import org.babyfish.jimmer.sql.dialect.PostgresDialect;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ExecutorForLogTest {

    @Test
    public void testPostgresArrayInCursorLog() {
        CapturingExecutor executor = new CapturingExecutor();
        JSqlClientImplementor sqlClient = (JSqlClientImplementor) JSqlClient.newBuilder()
                .setDialect(new PostgresDialect())
                .setSqlFormatter(SqlFormatter.INLINE_PRETTY)
                .build();
        String sql = "select * from BOOK_STORE where ID = any(?)";
        executor.openCursor(
                1L,
                sql,
                Collections.singletonList(new TypedList<>("bigint", new Long[] {1L, 2L})),
                Collections.singletonList(sql.indexOf('?') + 1),
                ExecutionPurpose.QUERY,
                null,
                sqlClient
        );
        Assertions.assertEquals(
                "Open cursor(1)===>\n" +
                        "Purpose: QUERY\n" +
                        "SQL: select * from BOOK_STORE where ID = any(ARRAY[1, 2]::bigint[])\n",
                executor.message
        );
    }

    @Test
    public void testPostgresInlineSqlMatchesJdbc() throws Exception {
        NativeDatabases.assumeNativeDatabase();
        JSqlClientImplementor sqlClient = (JSqlClientImplementor) JSqlClient.newBuilder()
                .setDialect(new PostgresDialect())
                .setSqlFormatter(SqlFormatter.INLINE_PRETTY)
                .build();
        List<TypedList<?>> arrays = Arrays.asList(
                new TypedList<>("bigint", new Long[] {1L, 2L, null}),
                new TypedList<>("bigint", new Long[] {}),
                new TypedList<>("bigint", new Long[] {null, null}),
                new TypedList<>("numeric", new BigDecimal[] {new BigDecimal("1.20"), new BigDecimal("2.50")}),
                new TypedList<>("float8", new Double[] {1.25, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}),
                new TypedList<>("uuid", new UUID[] {UUID.fromString("550e8400-e29b-41d4-a716-446655440000"), null}),
                new TypedList<>("uuid", new UUID[] {}),
                new TypedList<>("boolean", new Boolean[] {true, false, null}),
                new TypedList<>("timestamp", new LocalDateTime[] {LocalDateTime.parse("2026-10-03T12:34:56.123456")}),
                new TypedList<>("timestamptz", new OffsetDateTime[] {OffsetDateTime.parse("2026-10-03T12:34:56.123456+03:00")}),
                new TypedList<>("text", new String[] {"O'Reilly", "a,b", "{x}", "\"quoted\"", "a\\b", null, "NULL", "", " 中文 "}),
                new TypedList<>("text", new String[] {"line\ntab\treturn\rback\bform\fend", "\\'"}),
                new TypedList<>("text", new String[] {})
        );
        try (Connection con = NativeDatabases.POSTGRES_DATA_SOURCE.getConnection(); Statement statement = con.createStatement()) {
            for (String setting : Arrays.asList("on", "off")) {
                statement.execute("set standard_conforming_strings = " + setting);
                for (TypedList<?> array : arrays) {
                    String sql = "select ?::" + array.getSqlElementType() + "[]";
                    assertInlineMatchesJdbc(con, sqlClient, sql, sql.length(), array);
                }
                String sql = "select array_agg(ID order by ID) " +
                        "from (values (1::bigint), (2::bigint), (3::bigint)) t(ID) where ID = any(?)";
                Assertions.assertEquals("{1,2}", assertInlineMatchesJdbc(
                        con, sqlClient, sql, sql.indexOf('?') + 1, new TypedList<>("bigint", new Long[] {1L, 2L})
                ));
            }
        }
    }

    private static String assertInlineMatchesJdbc(
            Connection con,
            JSqlClientImplementor sqlClient,
            String sql,
            int variablePosition,
            TypedList<?> array
    ) throws Exception {
        CapturingExecutor executor = new CapturingExecutor();
        String jdbcResult = executor.execute(new Executor.Args<>(
                sqlClient,
                con,
                sql,
                Collections.singletonList(array),
                Collections.singletonList(variablePosition),
                ExecutionPurpose.QUERY,
                null,
                null,
                (stmt, args) -> {
                    try (ResultSet rs = stmt.executeQuery()) {
                        Assertions.assertTrue(rs.next());
                        return rs.getString(1);
                    }
                }
        ));
        int start = executor.message.indexOf("SQL: ") + 5;
        String inlineSql = executor.message.substring(start, executor.message.indexOf('\n', start));
        try (Statement statement = con.createStatement(); ResultSet rs = statement.executeQuery(inlineSql)) {
            Assertions.assertTrue(rs.next());
            Assertions.assertEquals(jdbcResult, rs.getString(1), inlineSql);
        }
        return jdbcResult;
    }

    private static class CapturingExecutor extends ExecutorForLog {

        private String message;

        CapturingExecutor() {
            super(DefaultExecutor.INSTANCE, (Logger) Proxy.newProxyInstance(
                    Logger.class.getClassLoader(),
                    new Class<?>[] {Logger.class},
                    (proxy, method, args) -> method.getReturnType() == boolean.class ? true : null
            ));
        }

        @Override
        protected void log(String message, Object... args) {
            this.message = message;
        }
    }
}
