package org.babyfish.jimmer.spring.repository;

import org.babyfish.jimmer.sql.runtime.ConnectionManager;
import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.function.Function;

public final class SpringConnectionManager implements ConnectionManager {

    private final DataSource dataSource;

    public SpringConnectionManager(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public <R> R execute(Function<Connection, R> block) {
        Connection con = DataSourceUtils.getConnection(dataSource);
        try {
            return block.apply(con);
        } finally {
            DataSourceUtils.releaseConnection(con, dataSource);
        }
    }
}
