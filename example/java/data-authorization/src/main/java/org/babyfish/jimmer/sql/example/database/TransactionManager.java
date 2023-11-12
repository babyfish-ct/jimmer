package org.babyfish.jimmer.sql.example.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Supplier;

public class TransactionManager {

    private final ThreadLocal<Connection> connectionLocal = new ThreadLocal<>();

    Connection currentConnection() {
        Connection con = connectionLocal.get();
        if (con == null) {
            throw new IllegalStateException("No transaction context");
        }
        return con;
    }

    public <R> R execute(Supplier<R> lambda) {
        Connection con = connectionLocal.get();
        if (con != null) {
            return lambda.get();
        }
        try {
            con = DataSources.DATA_SOURCE.getConnection();
            try {
                con.setAutoCommit(false);
                R result;
                try {
                    connectionLocal.set(con);
                    try {
                        result = lambda.get();
                    } finally {
                        connectionLocal.remove();
                    }
                } catch (RuntimeException | Error ex) {
                    con.rollback();
                    throw ex;
                }
                con.commit();
                return result;
            } finally {
                con.close();
            }
        } catch (SQLException ex) {
            throw new RuntimeException("JDBC Error: " + ex.getMessage(), ex);
        }
    }
}
