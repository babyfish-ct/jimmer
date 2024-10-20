package org.babyfish.jimmer.sql.runtime;

import org.jetbrains.annotations.Nullable;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.function.Function;

@FunctionalInterface
public interface ConnectionManager {

    ConnectionManager EXTERNAL_ONLY = new ConnectionManager() {
        @Override
        public <R> R execute(@Nullable Connection con, Function<Connection, R> block) {
            Objects.requireNonNull(con, "External connection cannot be null");
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

    static ConnectionManager simpleConnectionManager(DataSource dataSource) {
        return new ConnectionManager() {

            private final ThreadLocal<Connection> local = new ThreadLocal<>();

            @Override
            public <R> R execute(@Nullable Connection con, Function<Connection, R> block) {
                if (con == null) {
                    return execute(block);
                }
                return block.apply(con);
            }

            @Override
            public <R> R execute(Function<Connection, R> block) {
                Connection con = local.get();
                if (con != null) {
                    return block.apply(con);
                }
                try {
                    con = dataSource.getConnection();
                    try {
                        local.set(con);
                        try {
                            return block.apply(con);
                        } finally {
                            local.remove();
                        }
                    } finally {
                        con.close();
                    }
                } catch (SQLException ex) {
                    throw new ExecutionException("Cannot open connection from datasource", ex);
                }
            }
        };
    }

    <R> R execute(@Nullable Connection con, Function<Connection, R> block);

    default <R> R execute(Function<Connection, R> block) {
        return execute(null, block);
    }
}
