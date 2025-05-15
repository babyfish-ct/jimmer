package org.babyfish.jimmer.sql.runtime;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
}
