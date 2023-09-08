package org.babyfish.jimmer.sql.runtime;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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
}
