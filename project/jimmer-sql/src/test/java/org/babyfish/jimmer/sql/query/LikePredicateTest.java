package org.babyfish.jimmer.sql.query;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.LikeMode;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.dialect.Dialect;
import org.babyfish.jimmer.sql.dialect.H2Dialect;
import org.babyfish.jimmer.sql.dialect.SQLiteDialect;
import org.babyfish.jimmer.sql.model.BookStoreTable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

public class LikePredicateTest {

    private static final List<String> NAMES = Arrays.asList(
            "50%", "50%done", "prefix50%done", "50\\", "50\\done", "50\\%", "50\\%done",
            "Fee%", "Fee%later", "prefixFee%later", "plain", ""
    );

    @Test
    public void testEscapedPercentMatchModes() throws Exception {
        try (Connection con = connection("jdbc:h2:mem:")) {
            assertMatches(con, new H2Dialect(), "50\\%", LikeMode.ANYWHERE, false, false, "50%", "50%done", "prefix50%done");
            assertMatches(con, new H2Dialect(), "50\\%", LikeMode.START, false, false, "50%", "50%done");
            assertMatches(con, new H2Dialect(), "50\\%", LikeMode.END, false, false, "50%");
            assertMatches(con, new H2Dialect(), "50\\%", LikeMode.EXACT, false, false, "50%");
        }
    }

    @Test
    public void testEscapedEscapeCharacter() throws Exception {
        try (Connection con = connection("jdbc:h2:mem:")) {
            assertMatches(con, new H2Dialect(), "50\\\\%", LikeMode.ANYWHERE, false, false,
                    "50\\", "50\\done", "50\\%", "50\\%done");
            assertMatches(con, new H2Dialect(), "50\\\\\\%", LikeMode.ANYWHERE, false, false, "50\\%", "50\\%done");
        }
    }

    @Test
    public void testInsensitiveAndNegative() throws Exception {
        try (Connection con = connection("jdbc:h2:mem:")) {
            Dialect[] dialects = {
                    new H2Dialect(),
                    new H2Dialect() {
                        @Override
                        public boolean isIgnoreCaseLikeSupported() {
                            return false;
                        }
                    }
            };
            for (Dialect dialect : dialects) {
                assertMatches(con, dialect, "FEE\\%", LikeMode.ANYWHERE, true, false, "Fee%", "Fee%later", "prefixFee%later");
                assertMatches(con, dialect, "FEE\\%", LikeMode.ANYWHERE, true, true,
                        "50%", "50%done", "prefix50%done", "50\\", "50\\done", "50\\%", "50\\%done", "plain", "");
            }
            assertMatches(con, new H2Dialect(), "50\\%", LikeMode.ANYWHERE, false, true,
                    "50\\", "50\\done", "50\\%", "50\\%done", "Fee%", "Fee%later", "prefixFee%later", "plain", "");
        }
    }

    @Test
    public void testDialectWithoutImplicitEscape() throws Exception {
        try (Connection con = connection("jdbc:sqlite::memory:")) {
            assertMatches(con, new SQLiteDialect(), "50\\%", LikeMode.ANYWHERE, false, false,
                    "50\\", "50\\done", "50\\%", "50\\%done");
        }
    }

    @Test
    public void testCustomEscapeCharacter() throws Exception {
        try (Connection con = connection("jdbc:h2:mem:;DEFAULT_ESCAPE=!")) {
            assertMatches(con, new H2Dialect(), "50!%", LikeMode.ANYWHERE, false, false, "50%", "50%done", "prefix50%done");
        }
    }

    @Test
    public void testUnescapedWildcardsAndEmptyPattern() throws Exception {
        try (Connection con = connection("jdbc:h2:mem:")) {
            assertMatches(con, new H2Dialect(), "%", LikeMode.ANYWHERE, false, false, NAMES.toArray(new String[0]));
            assertMatches(con, new H2Dialect(), "", LikeMode.ANYWHERE, false, false, NAMES.toArray(new String[0]));
            assertMatches(con, new H2Dialect(), "", LikeMode.EXACT, false, false, "");
            assertMatches(con, new H2Dialect(), "Fee%", LikeMode.START, false, false, "Fee%", "Fee%later");
            assertMatches(con, new H2Dialect(), "F_e%", LikeMode.ANYWHERE, false, false, "Fee%", "Fee%later", "prefixFee%later");
        }
    }

    private static Connection connection(String url) throws Exception {
        Connection con = DriverManager.getConnection(url);
        try (Statement statement = con.createStatement()) {
            statement.execute("create table BOOK_STORE(NAME varchar(100))");
        }
        try (PreparedStatement statement = con.prepareStatement("insert into BOOK_STORE(NAME) values (?)")) {
            for (String name : NAMES) {
                statement.setString(1, name);
                statement.executeUpdate();
            }
        }
        return con;
    }

    private static void assertMatches(
            Connection con,
            Dialect dialect,
            String pattern,
            LikeMode mode,
            boolean insensitive,
            boolean negative,
            String... expected
    ) {
        BookStoreTable table = BookStoreTable.$;
        Predicate predicate = insensitive ? table.name().ilike(pattern, mode) : table.name().like(pattern, mode);
        List<String> actual = JSqlClient.newBuilder().setDialect(dialect).build()
                .createQuery(table)
                .where(negative ? Predicate.not(predicate) : predicate)
                .select(table.name())
                .execute(con);
        Assertions.assertEquals(new TreeSet<>(Arrays.asList(expected)), new TreeSet<>(actual), pattern + ": " + mode);
    }
}
