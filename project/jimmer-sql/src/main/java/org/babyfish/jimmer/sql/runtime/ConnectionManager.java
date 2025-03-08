package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.sql.exception.ExecutionException;
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

    static TransactionalConnectionManager simpleConnectionManager(DataSource dataSource) {
        return new AbstractTransactionalConnectionManager() {

            @Override
            protected Connection openConnection() throws SQLException {
                return dataSource.getConnection();
            }
        };
    }
}
