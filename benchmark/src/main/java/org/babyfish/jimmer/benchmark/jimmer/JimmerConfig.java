package org.babyfish.jimmer.benchmark.jimmer;

import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.dialect.H2Dialect;
import org.babyfish.jimmer.sql.runtime.ConnectionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.function.Function;

@Configuration
public class JimmerConfig {

    @Bean
    public SqlClient sqlClient(
            DataSource dataSource
    ) {
        return SqlClient
                .newBuilder()
                .setDialect(new H2Dialect())
                .setConnectionManager(new ConnectionManager() {
                    @Override
                    public <R> R execute(Function<Connection, R> block) {
                        Connection con = DataSourceUtils.getConnection(dataSource);
                        try {
                            return block.apply(con);
                        } finally {
                            DataSourceUtils.releaseConnection(con, dataSource);
                        }
                    }
                })
                .build();
    }
}
