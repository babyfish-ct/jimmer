package org.babyfish.jimmer.sql.runtime;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.function.Function;

@FunctionalInterface
public interface ConnectionManager {

    <R> R execute(Function<Connection, R> block);

    static ConnectionManager simpleConnectionManager(DataSource dataSource) {
        return new ConnectionManager() {
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

    ConnectionManager ILLEGAL = new ConnectionManager() {
        @Override
        public <R> R execute(Function<Connection, R> block) {
            throw new ExecutionException("ConnectionManager of SqlClient is not configured");
        }
    };
}
