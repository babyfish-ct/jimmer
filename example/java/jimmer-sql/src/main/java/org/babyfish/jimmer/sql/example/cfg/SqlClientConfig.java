package org.babyfish.jimmer.sql.example.cfg;

import org.babyfish.jimmer.sql.DraftInterceptor;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.cache.CacheFactory;
import org.babyfish.jimmer.sql.dialect.H2Dialect;
import org.babyfish.jimmer.sql.dialect.MySqlDialect;
import org.babyfish.jimmer.sql.example.model.JimmerModule;
import org.babyfish.jimmer.sql.filter.Filter;
import org.babyfish.jimmer.sql.runtime.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Function;

@Configuration
public class SqlClientConfig {

    @Bean
    public JSqlClient sqlClient(
            DataSource dataSource,
            @Value("${spring.datasource.url}") String jdbcUrl,
            List<DraftInterceptor<?>> interceptors,
            List<Filter<?>> filters,
            @Autowired(required = false) CacheFactory cacheFactory
    ) {
        boolean isH2 = jdbcUrl.startsWith("jdbc:h2:");
        JSqlClient sqlClient = JSqlClient.newBuilder()
                .setConnectionManager(
                        /*
                         * It's very important to use
                         *      "org.springframework.jdbc.datasource.DataSourceUtils"!
                         * This is spring transaction aware ConnectionManager
                         */
                        new ConnectionManager() {
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
                )
                .setDialect(isH2 ? new H2Dialect() : new MySqlDialect())
                .setExecutor(Executor.log())
                .addDraftInterceptors(interceptors)
                .addFilters(filters)
                .setEntityManager(JimmerModule.ENTITY_MANAGER)
                .setCaches(it -> {
                    if (cacheFactory != null) {
                        it.setCacheFactory(cacheFactory);
                    }
                })
                .build();
        if (isH2) {
            initializeH2Database(sqlClient);
        }
        return sqlClient;
    }

    private void initializeH2Database(JSqlClient sqlClient) {
        sqlClient.getConnectionManager().execute(con -> {
            InputStream inputStream = SqlClientConfig.class
                    .getClassLoader()
                    .getResourceAsStream("h2-database.sql");
            if (inputStream == null) {
                throw new RuntimeException("no h2-database.sql");
            }
            try (Reader reader = new InputStreamReader(inputStream)) {
                char[] buf = new char[1024];
                StringBuilder builder = new StringBuilder();
                while (true) {
                    int len = reader.read(buf);
                    if (len == -1) {
                        break;
                    }
                    builder.append(buf, 0, len);
                }
                con.createStatement().execute(builder.toString());
            } catch (IOException | SQLException ex) {
                throw new RuntimeException("Cannot initialize h2 database", ex);
            }
            return null;
        });
    }
}
