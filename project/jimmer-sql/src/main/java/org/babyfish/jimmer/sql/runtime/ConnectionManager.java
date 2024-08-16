package org.babyfish.jimmer.sql.runtime;

import org.jetbrains.annotations.Nullable;

import javax.sql.DataSource;
import java.sql.Connection;
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
        return new ConnectionManager() {
            @Override
            public <R> R execute(@Nullable Connection con, Function<Connection, R> block) {
                return block.apply(con == null ? connection : con);
            }
        };
    }

    static ConnectionManager simpleConnectionManager(DataSource dataSource) {
        return new ConnectionManager() {
            @Override
            public <R> R execute(@Nullable Connection con, Function<Connection, R> block) {
                try {
                    return block.apply(con);
                } catch (RuntimeException | Error ex) {
                    throw ex;
                } catch (Throwable ex) {
                    throw new ExecutionException(ex.getMessage(), ex);
                }
            }

            @Override
            public <R> R execute(Function<Connection, R> block) {
                try (Connection con = dataSource.getConnection()) {
                    return block.apply(con);
                } catch (RuntimeException | Error ex) {
                    throw ex;
                } catch (Throwable ex) {
                    throw new ExecutionException(ex.getMessage(), ex);
                }
            }
        };
    }

    <R> R execute(@Nullable Connection con, Function<Connection, R> block);

    default <R> R execute(Function<Connection, R> block) {
        return execute(null, block);
    }
}
