package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.sql.transaction.AbstractTxConnectionManager;
import org.babyfish.jimmer.sql.transaction.TxConnectionManager;
import org.jetbrains.annotations.Nullable;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.function.Function;

@FunctionalInterface
public interface ConnectionManager {

    <R> R execute(@Nullable Connection con, Function<Connection, R> block);

    default <R> R execute(Function<Connection, R> block) {
        return execute(null, block);
    }

    ConnectionManager EXTERNAL_ONLY = new ConnectionManager() {
        @Override
        public <R> R execute(@Nullable Connection con, Function<Connection, R> block) {
            if (con == null) {
                throw new IllegalArgumentException(
                        "The connection manager is not specified " +
                                "so \"ConnectionManager.EXTERNAL_ONLY\" " +
                                "which does not support no explicit JDBC " +
                                "connection execution is used as default. " +
                                "There are 2 choices: " +
                                "1. Specify the connection when execute statement/command" +
                                "2. Specify the connection manager"
                );
            }
            return block.apply(con);
        }
    };

    static ConnectionManager singleConnectionManager(Connection connection) {
        if (connection == null) {
            return EXTERNAL_ONLY;
        }
        return new ConnectionManager() {
            @Override
            public <R> R execute(@Nullable Connection con, Function<Connection, R> block) {
                return block.apply(con == null ? connection : con);
            }
        };
    }

    static TxConnectionManager simpleConnectionManager(DataSource dataSource) {
        return new AbstractTxConnectionManager() {

            @Override
            protected Connection openConnection() throws SQLException {
                return dataSource.getConnection();
            }
        };
    }
}
