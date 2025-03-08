package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.sql.exception.ExecutionException;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Function;

public abstract class AbstractTransactionalConnectionManager implements TransactionalConnectionManager {

    private final ThreadLocal<Scope> scopeLocal = new ThreadLocal<>();

    @Override
    public final <R> R execute(@Nullable Connection con, Function<Connection, R> block) {
        if (con != null) {
            // No connection management, no transaction management, everything is controlled by user.
            return block.apply(con);
        }
        return executeTransaction(Propagation.SUPPORTS, block);
    }

    @Override
    public final <R> R execute(Function<Connection, R> block) {
        return executeTransaction(Propagation.SUPPORTS, block);
    }

    @Override
    public final <R> R executeTransaction(Function<Connection, R> block) {
        return executeTransaction(Propagation.REQUIRED, block);
    }

    @Override
    public final <R> R executeTransaction(Propagation propagation, Function<Connection, R> block) {
        try {
            Scope parent = scopeLocal.get();
            Scope scope = createScope(parent, propagation);
            scopeLocal.set(scope);
            try {
                R result;
                try {
                    result = execute(scope.con, block);
                } catch (RuntimeException | Error ex) {
                    scope.terminate(true);
                    throw ex;
                }
                scope.terminate(false);
                return result;
            } finally {
                if (parent != null) {
                    scopeLocal.set(parent);
                } else {
                    scopeLocal.remove();
                }
            }
        } catch (SQLException ex) {
            throw new ExecutionException("JDBC error raised: " + ex.getMessage(), ex);
        }
    }

    protected abstract Connection openConnection() throws SQLException;

    protected void closeConnection(Connection con) throws SQLException {
        con.close();
    }

    protected void startTransaction(Connection con) throws SQLException {
        con.setAutoCommit(false);
    }

    protected void commitTransaction(Connection con) throws SQLException {
        con.commit();
    }

    protected void rollbackTransaction(Connection con) throws SQLException {
        con.rollback();
    }

    protected void abortTransaction(Connection con) throws SQLException {
        // The connection is borrowed from parent scope
        // However, the parent scope does not have transaction
        con.setAutoCommit(true);
    }

    private Scope createScope(Scope parent, Propagation propagation) throws SQLException {
        switch (propagation) {
            case REQUIRES_NEW:
                return new Scope(parent, false, true);
            case SUPPORTS:
                return new Scope(parent, true, parent != null && parent.withTransaction);
            case NOT_SUPPORTED:
                return new Scope(parent, true, false);
            case MANDATORY:
                if (parent == null || !parent.withTransaction) {
                    throw new ExecutionException(
                            "The transaction propagation is \"MANDATORY\" but there is no transaction context"
                    );
                }
                return new Scope(parent, true, true);
            case NEVER:
                if (parent != null && parent.withTransaction) {
                    throw new ExecutionException(
                            "The transaction propagation is \"NEVER\" but there is already a transaction context"
                    );
                }
                return new Scope(parent, true, false);
            default:
                // REQUIRED
                return new Scope(parent, true, true);
        }
    }

    private class Scope {

        private final Connection con;

        private final boolean withTransaction;

        private final boolean connectionOwner;

        private final boolean transactionOwner;

        Scope(Scope parent, boolean borrow, boolean withTransaction) throws SQLException {
            if (parent != null && parent.withTransaction && !withTransaction) {
                borrow = false;
            }
            Connection con;
            if (parent != null && borrow) {
                con = parent.con;
                this.connectionOwner = false;
            } else {
                con = openConnection();
                this.connectionOwner = true;
            }
            this.withTransaction = withTransaction;
            if (!withTransaction) {
                transactionOwner = false;
            } else if (connectionOwner) {
                transactionOwner = true;
            }else {
                transactionOwner = !parent.withTransaction;
            }
            if (transactionOwner) {
                try {
                    startTransaction(con);
                } catch (SQLException | RuntimeException | Error ex) {
                    closeConnection(con);
                    this.con = null;
                    throw ex;
                }
            }
            this.con = con;
        }

        void terminate(boolean error) throws SQLException {
            Connection con = this.con;
            if (con == null) {
                return;
            }
            try {
                if (transactionOwner) {
                    if (error) {
                        rollbackTransaction(con);
                    } else {
                        commitTransaction(con);
                    }
                    if (!connectionOwner) {
                        abortTransaction(con);
                    }
                }
            } finally {
                if (connectionOwner) {
                    closeConnection(con);
                }
            }
        }
    }
}
