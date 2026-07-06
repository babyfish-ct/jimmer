package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.sql.common.ProxyRecorder;
import org.babyfish.jimmer.sql.dialect.PostgresDialect;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Savepoint;
import java.util.Collections;

public class SavepointManagerTest {

    @Test
    public void testBatchWithoutSavepoint() {
        ProxyRecorder<Connection> con = ProxyRecorder.of(Connection.class);
        ProxyRecorder<PreparedStatement> stmt = ProxyRecorder.of(PreparedStatement.class);
        con.returns("prepareStatement", stmt.proxy());
        stmt.returns("executeBatch", new int[] {1});

        try (Executor.BatchContext ctx = DefaultExecutor.INSTANCE.executeBatch(
                con.proxy(),
                "insert into test_table(id) values(?)",
                null,
                ExecutionPurpose.MUTATE,
                sqlClient(),
                false
        )) {
            ctx.add(Collections.emptyList());
            Assertions.assertArrayEquals(new int[] {1}, ctx.execute(null));
        }
        con.assertCalledOnce("prepareStatement");
        con.assertNeverCalled("getAutoCommit");
        con.assertNeverCalled("setSavepoint");
        con.assertNeverCalled("releaseSavepoint");
        stmt.assertCalledOnce("addBatch");
        stmt.assertCalledOnce("executeBatch");
        stmt.assertCalledOnce("close");
    }

    @Test
    public void testBatchWithSavepoint() {
        ProxyRecorder<Connection> con = ProxyRecorder.of(Connection.class);
        ProxyRecorder<PreparedStatement> stmt = ProxyRecorder.of(PreparedStatement.class);
        Savepoint savepoint = ProxyRecorder.of(Savepoint.class).proxy();
        con.returns("getAutoCommit", false);
        con.returns("setSavepoint", savepoint);
        con.returns("prepareStatement", stmt.proxy());
        stmt.returns("getConnection", con.proxy());
        stmt.returns("executeBatch", new int[] {1});

        try (Executor.BatchContext ctx = DefaultExecutor.INSTANCE.executeBatch(
                con.proxy(),
                "insert into test_table(id) values(?)",
                null,
                ExecutionPurpose.MUTATE,
                sqlClient(),
                true
        )) {
            ctx.add(Collections.emptyList());
            Assertions.assertArrayEquals(new int[] {1}, ctx.execute(null));
        }
        con.assertCalledOnce("getAutoCommit");
        con.assertCalledOnce("setSavepoint");
        con.assertCalledOnceWith("releaseSavepoint", savepoint);
        stmt.assertCalledOnce("addBatch");
        stmt.assertCalledOnce("executeBatch");
        stmt.assertCalledOnce("close");
    }

    private static JSqlClientImplementor sqlClient() {
        ProxyRecorder<JSqlClientImplementor> sqlClient = ProxyRecorder.of(JSqlClientImplementor.class);
        sqlClient.returns("getDialect", new PostgresDialect());
        sqlClient.returns("isConstraintViolationTranslatable", true);
        return sqlClient.proxy();
    }
}
