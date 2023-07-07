package org.babyfish.jimmer.benchmark.jimmer;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.dialect.H2Dialect;
import org.babyfish.jimmer.sql.runtime.ConnectionManager;
import org.babyfish.jimmer.sql.runtime.EntityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.function.Function;

@Configuration
public class JimmerConfig {

    @Bean
    public JSqlClient sqlClient(
            DataSource dataSource
    ) {
        return JSqlClient
                .newBuilder()
                .setDialect(new H2Dialect())
                .setEntityManager(new EntityManager(JimmerData.class))
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
