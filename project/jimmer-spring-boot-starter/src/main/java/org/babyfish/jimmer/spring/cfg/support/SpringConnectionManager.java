package org.babyfish.jimmer.spring.cfg.support;

import org.jetbrains.annotations.NotNull;
import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.function.Function;

public class SpringConnectionManager implements DataSourceAwareConnectionManager {

    private final DataSource dataSource;

    public SpringConnectionManager(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @NotNull
    @Override
    public DataSource getDataSource() {
        return dataSource;
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
