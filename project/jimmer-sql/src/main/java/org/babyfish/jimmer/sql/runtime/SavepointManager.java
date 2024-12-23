package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.sql.exception.ExecutionException;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;

public class SavepointManager {
    private SavepointManager() {
    }

    @Nullable
    public static Savepoint setIfNeeded(Connection con, JSqlClientImplementor sqlClient) {
        if (sqlClient.getDialect().isTransactionAbortedByError()) {
            try {
                return con.getAutoCommit() ? null : con.setSavepoint();
            } catch (SQLException ex) {
                throw new ExecutionException("Failed to set savepoint", ex);
            }
        }
        return null;
    }

    public static void rollback(ConnectionSupplier con, @Nullable Savepoint savepoint) {
        if (savepoint != null) {
            try {
                con.get().rollback(savepoint);
            } catch (SQLException e) {
                throw new ExecutionException("Failed to rollback to savepoint", e);
            }
        }
    }

    public static void release(ConnectionSupplier con, @Nullable Savepoint savepoint) {
        if (savepoint != null) {
            try {
                con.get().releaseSavepoint(savepoint);
            } catch (SQLException e) {
                throw new ExecutionException("Failed to release savepoint", e);
            }
        }
    }

    @FunctionalInterface
    public interface ConnectionSupplier {
        Connection get() throws SQLException;
    }
}
