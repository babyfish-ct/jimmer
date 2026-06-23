package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.sql.dialect.PostgresDialect;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Savepoint;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SavepointManagerTest {

    @Test
    public void testBatchWithoutSavepoint() throws Exception {
        Connection con = mock(Connection.class);
        PreparedStatement stmt = mock(PreparedStatement.class);
        when(con.prepareStatement(anyString())).thenReturn(stmt);
        when(stmt.executeBatch()).thenReturn(new int[] {1});

        try (Executor.BatchContext ctx = DefaultExecutor.INSTANCE.executeBatch(
                con,
                "insert into test_table(id) values(?)",
                null,
                ExecutionPurpose.MUTATE,
                sqlClient(),
                false
        )) {
            ctx.add(Collections.emptyList());
            Assertions.assertArrayEquals(new int[] {1}, ctx.execute(null));
        }
        verify(con).prepareStatement(anyString());
        verify(con, never()).getAutoCommit();
        verify(con, never()).setSavepoint();
        verify(con, never()).releaseSavepoint(any(Savepoint.class));
        verify(stmt).addBatch();
        verify(stmt).executeBatch();
        verify(stmt).close();
    }

    @Test
    public void testBatchWithSavepoint() throws Exception {
        Connection con = mock(Connection.class);
        PreparedStatement stmt = mock(PreparedStatement.class);
        Savepoint savepoint = mock(Savepoint.class);
        when(con.getAutoCommit()).thenReturn(false);
        when(con.setSavepoint()).thenReturn(savepoint);
        when(con.prepareStatement(anyString())).thenReturn(stmt);
        when(stmt.getConnection()).thenReturn(con);
        when(stmt.executeBatch()).thenReturn(new int[] {1});

        try (Executor.BatchContext ctx = DefaultExecutor.INSTANCE.executeBatch(
                con,
                "insert into test_table(id) values(?)",
                null,
                ExecutionPurpose.MUTATE,
                sqlClient(),
                true
        )) {
            ctx.add(Collections.emptyList());
            Assertions.assertArrayEquals(new int[] {1}, ctx.execute(null));
        }
        verify(con).getAutoCommit();
        verify(con).setSavepoint();
        verify(con).releaseSavepoint(savepoint);
        verify(stmt).addBatch();
        verify(stmt).executeBatch();
        verify(stmt).close();
    }

    private static JSqlClientImplementor sqlClient() {
        JSqlClientImplementor sqlClient = mock(JSqlClientImplementor.class);
        when(sqlClient.getDialect()).thenReturn(new PostgresDialect());
        when(sqlClient.getExecutorContextPrefixes()).thenReturn(null);
        when(sqlClient.isConstraintViolationTranslatable()).thenReturn(true);
        return sqlClient;
    }
}
