package org.babyfish.jimmer.sql.dialect;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.impl.query.ForUpdate;
import org.babyfish.jimmer.sql.ast.impl.render.BatchSqlBuilder;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.ast.query.ConfigurableRootQuery;
import org.babyfish.jimmer.sql.ast.query.LockMode;
import org.babyfish.jimmer.sql.ast.query.LockWait;
import org.babyfish.jimmer.sql.cache.TransactionCacheOperator;
import org.babyfish.jimmer.sql.common.AbstractTest;
import org.babyfish.jimmer.sql.model.middle.Card;
import org.babyfish.jimmer.sql.model.middle.CardDraft;
import org.babyfish.jimmer.sql.model.middle.CardTable;
import org.babyfish.jimmer.sql.runtime.ConnectionManager;
import org.babyfish.jimmer.sql.runtime.EntityManager;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

public class MariaDbDialectTest extends AbstractTest {

    @Test
    public void testLockSyntax() {
        MariaDbDialect baseline = new MariaDbDialect();
        assertEquals(" for update", render(baseline, LockMode.UPDATE, LockWait.DEFAULT));
        assertEquals(" lock in share mode", render(baseline, LockMode.SHARE, LockWait.DEFAULT));
        assertThrows(IllegalArgumentException.class, () -> render(baseline, LockMode.UPDATE, LockWait.NO_WAIT));
        assertThrows(IllegalArgumentException.class, () -> render(baseline, LockMode.PG_KEY_SHARE, LockWait.DEFAULT));

        MariaDbDialect old = new MariaDbDialect(10, 2);
        assertThrows(IllegalArgumentException.class, () -> render(old, LockMode.UPDATE, LockWait.seconds(1)));
        MariaDbDialect withTimeout = new MariaDbDialect(10, 3);
        assertEquals(" for update nowait", render(withTimeout, LockMode.UPDATE, LockWait.NO_WAIT));
        assertEquals(" lock in share mode wait 2", render(withTimeout, LockMode.SHARE, LockWait.seconds(2)));
        assertThrows(IllegalArgumentException.class, () -> render(withTimeout, LockMode.UPDATE, LockWait.SKIP_LOCKED));
        assertEquals(" for update skip locked", render(new MariaDbDialect(10, 6), LockMode.UPDATE, LockWait.SKIP_LOCKED));
        assertEquals(" for update skip locked", render(new MariaDbDialect(11, 0), LockMode.UPDATE, LockWait.SKIP_LOCKED));
    }

    private String render(MariaDbDialect dialect, LockMode mode, LockWait wait) {
        BatchSqlBuilder builder = new BatchSqlBuilder((JSqlClientImplementor) getSqlClient());
        dialect.renderForUpdate(builder, new ForUpdate(mode, wait));
        return builder.build().get_1();
    }

    // Use a jdbc:mysql: URL for an empty MariaDB database with the test suite's MySQL JDBC driver.
    @Test
    @EnabledIfEnvironmentVariable(named = "JIMMER_TEST_JDBC_URL", matches = ".+")
    public void testNativeMariaDb() throws SQLException {
        try (Connection con = DriverManager.getConnection(
                System.getenv("JIMMER_TEST_JDBC_URL"),
                System.getenv("JIMMER_TEST_JDBC_USER"),
                System.getenv("JIMMER_TEST_JDBC_PASSWORD")
        ); Statement statement = con.createStatement()) {
            try (ResultSet tables = statement.executeQuery("show tables")) {
                assertFalse(tables.next(), "Use an empty, disposable MariaDB database");
            }
            int major;
            int minor;
            try (ResultSet rs = statement.executeQuery("select version()")) {
                assertTrue(rs.next());
                String version = rs.getString(1);
                assertTrue(version.contains("MariaDB"), version);
                String[] parts = version.split("\\.");
                major = Integer.parseInt(parts[0]);
                minor = Integer.parseInt(parts[1]);
            }
            JSqlClient client = getSqlClient(builder -> builder
                    .setEntityManager(new EntityManager(Card.class))
                    .setConnectionManager(ConnectionManager.singleConnectionManager(con))
                    .setDialect(new MariaDbDialect(major, minor))
            );
            statement.execute("create temporary table CARD (ID bigint primary key, NAME varchar(100)) engine=InnoDB");
            try {
                new TransactionCacheOperator().initialize(client);
                try (ResultSet rs = statement.executeQuery("select count(*) from " + TransactionCacheOperator.TABLE_NAME)) {
                    assertTrue(rs.next());
                    assertEquals(0, rs.getInt(1));
                }

                con.setAutoCommit(false);
                for (String name : new String[] { "inserted", "updated" }) {
                    Card card = CardDraft.$.produce(draft -> draft.setId(1L).setName(name));
                    client.saveCommand(card).setMode(SaveMode.UPSERT).execute();
                }
                assertEquals(2L, getExecutions().stream()
                        .filter(it -> it.getSql().contains(" on duplicate key update "))
                        .count());

                CardTable table = CardTable.$;
                ConfigurableRootQuery<CardTable, String> query = client.createQuery(table)
                        .where(table.id().eq(1L)).select(table.name());
                assertEquals("updated", query.forUpdate().fetchOne());
                assertEquals("updated", query.forUpdate(LockMode.SHARE, LockWait.DEFAULT).fetchOne());
                if (major > 10 || (major == 10 && minor >= 3)) {
                    assertEquals("updated", query.forUpdate(LockMode.UPDATE, LockWait.NO_WAIT).fetchOne());
                    assertEquals("updated", query.forUpdate(LockMode.SHARE, LockWait.seconds(1)).fetchOne());
                }
                if (major > 10 || (major == 10 && minor >= 6)) {
                    assertEquals("updated", query.forUpdate(LockMode.UPDATE, LockWait.SKIP_LOCKED).fetchOne());
                }
            } finally {
                if (!con.getAutoCommit()) {
                    con.rollback();
                }
                statement.execute("drop table if exists " + TransactionCacheOperator.TABLE_NAME);
            }
        }
    }
}
