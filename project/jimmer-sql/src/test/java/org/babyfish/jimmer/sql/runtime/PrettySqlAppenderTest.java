package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.sql.collection.TypedList;
import org.babyfish.jimmer.sql.dialect.Dialect;
import org.babyfish.jimmer.sql.dialect.H2Dialect;
import org.babyfish.jimmer.sql.dialect.PostgresDialect;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PrettySqlAppenderTest {

    private static final String SQL =
            "select * from BOOK " +
                    "where (name, edition) in (" +
                    "(?, ?)," +
                    "(?, ?)," +
                    "(?, ?)" +
                    ")";

    private static final List<Object> VARIABLES =
            Arrays.asList(
                    "Learning GraphQL", 3,
                    "GraphQL in Action", 3,
                    "Effective TypeScript", 3
            );

    private static final List<Integer> VARIABLE_POSITIONS =
            IntStream.range(0, SQL.length())
                    .filter(it -> SQL.charAt(it) == '?')
                    .mapToObj(it -> it + 1)
                    .collect(Collectors.toList());

    @Test
    public void testComment() {
        StringBuilder builder = new StringBuilder();
        PrettySqlAppender.comment(100).append(builder, SQL, VARIABLES, VARIABLE_POSITIONS);
        Assertions.assertEquals(
                "select * from BOOK where (name, edition) in (" +
                        "(? /* Learning GraphQL */, ? /* 3 */)," +
                        "(? /* GraphQL in Action */, ? /* 3 */)," +
                        "(? /* Effective TypeScript */, ? /* 3 */)" +
                        ")",
                builder.toString()
        );
    }

    @Test
    public void testInline() {
        StringBuilder builder = new StringBuilder();
        PrettySqlAppender.inline().append(builder, SQL, VARIABLES, VARIABLE_POSITIONS);
        Assertions.assertEquals(
                "select * from BOOK where (name, edition) in (" +
                        "('Learning GraphQL', 3)," +
                        "('GraphQL in Action', 3)," +
                        "('Effective TypeScript', 3)" +
                        ")",
                builder.toString()
        );
    }

    @Test
    public void testInlineWithJdbcParameter() {
        StringBuilder builder = new StringBuilder();
        List<Object> values = new ArrayList<>();
        List<Integer> positions = new ArrayList<>();
        builder.append("select * from X where id = ?::varbinary");
        values.add(new byte[] {1, 2});
        positions.add(builder.length());
        builder.append(" and deleted_uuid = ?::varbinary");
        values.add(new byte[] {3, 4});
        positions.add(builder.length());
        String sql = builder.toString();
        builder = new StringBuilder();
        PrettySqlAppender.inline().append(builder, sql, values, positions);
        Assertions.assertEquals(
                "select * from X " +
                        "where id = 0x0102 and deleted_uuid = 0x0304",
                builder.toString()
        );
    }

    @Test
    public void testPostgresNumericArrays() {
        assertArray("ARRAY[1, 2, NULL]::bigint[]", "bigint", 1L, 2L, null);
        assertArray("ARRAY[-1, 2]::int[]", "int", -1, 2);
        assertArray("ARRAY[1.20, 2.50]::numeric[]", "numeric", new BigDecimal("1.20"), new BigDecimal("2.50"));
        assertArray("ARRAY[1.25, 'NaN', 'Infinity', '-Infinity']::float8[]",
                "float8", 1.25, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY);
    }

    @Test
    public void testPostgresUuidArray() {
        assertArray("ARRAY['550e8400-e29b-41d4-a716-446655440000', NULL]::uuid[]",
                "uuid", UUID.fromString("550e8400-e29b-41d4-a716-446655440000"), null);
    }

    @Test
    public void testPostgresTextArray() {
        assertArray("ARRAY['O''Reilly', 'a,b', '{x}', '\"quoted\"', E'a\\\\b', NULL, 'NULL', '', ' 中文 ']::text[]",
                "text", "O'Reilly", "a,b", "{x}", "\"quoted\"", "a\\b", null, "NULL", "", " 中文 ");
        assertArray("ARRAY[E'line\\012tab\\011return\\015back\\010form\\014end', E'\\\\''']::text[]",
                "text", "line\ntab\treturn\rback\bform\fend", "\\'");
    }

    @Test
    public void testPostgresEmptyAndNullArrays() {
        assertArray("ARRAY[]::bigint[]", "bigint");
        assertArray("ARRAY[]::uuid[]", "uuid");
        assertArray("ARRAY[]::text[]", "text");
        assertArray("ARRAY[NULL, NULL]::bigint[]", "bigint", null, null);
    }

    @Test
    public void testPostgresBooleanArray() {
        assertArray("ARRAY[TRUE, FALSE, NULL]::boolean[]", "boolean", true, false, null);
    }

    @Test
    public void testPostgresArraySlice() {
        List<Long> slice = new TypedList<>("bigint", new Long[] {1L, 2L, 3L}).subList(1, 2);
        Assertions.assertEquals("select ARRAY[2]::bigint[]", format(SqlFormatter.INLINE_PRETTY, new PostgresDialect(), slice));
    }

    @Test
    public void testDialectContextIsPerCall() {
        TypedList<Long> values = new TypedList<>("bigint", new Long[] {1L, 2L});
        Assertions.assertEquals("select ARRAY[1, 2]::bigint[]", format(SqlFormatter.INLINE_PRETTY, new PostgresDialect(), values));
        Assertions.assertEquals("select '[1, 2]'", format(SqlFormatter.INLINE_PRETTY, new H2Dialect(), values));
        Assertions.assertEquals("select '[1, 2]'", format(SqlFormatter.INLINE_PRETTY, null, values));
        Assertions.assertEquals("select ? /* [1, 2] */", format(SqlFormatter.PRETTY, new PostgresDialect(), values));
        Assertions.assertEquals("select ?", format(SqlFormatter.SIMPLE, new PostgresDialect(), values));
    }

    @Test
    public void testScalarsWithDialectContext() {
        StringBuilder builder = new StringBuilder();
        SqlFormatter.INLINE_PRETTY.append(builder, SQL, VARIABLES, VARIABLE_POSITIONS, new PostgresDialect());
        Assertions.assertEquals(
                "select * from BOOK where (name, edition) in (" +
                        "('Learning GraphQL', 3),('GraphQL in Action', 3),('Effective TypeScript', 3))",
                builder.toString()
        );
        Assertions.assertEquals("select 'O''Reilly'", format(SqlFormatter.INLINE_PRETTY, new PostgresDialect(), "O'Reilly"));
    }

    private static void assertArray(String expected, String type, Object... values) {
        Assertions.assertEquals(
                "select " + expected,
                format(SqlFormatter.INLINE_PRETTY, new PostgresDialect(), new TypedList<>(type, values))
        );
    }

    private static String format(SqlFormatter formatter, Dialect dialect, Object value) {
        StringBuilder builder = new StringBuilder();
        formatter.append(builder, "select ?", Collections.singletonList(value), Collections.singletonList(8), dialect);
        return builder.toString();
    }
}
