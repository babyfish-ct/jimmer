package org.babyfish.jimmer.sql.example.graphql.cfg;

import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.dialect.H2Dialect;
import org.babyfish.jimmer.sql.example.graphql.entities.Gender;
import org.babyfish.jimmer.sql.runtime.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Function;

@Configuration
public class SqlClientConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqlClientConfig.class);

    @Bean
    public SqlClient sqlClient(DataSource dataSource) {
        return SqlClient.newBuilder()
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
                .setDialect(
                        new H2Dialect() // Support sequence
                )
                .setExecutor(
                        /*
                         * Log SQL and variables
                         */
                        new Executor() {
                            @Override
                            public <R> R execute(
                                    Connection con,
                                    String sql,
                                    List<Object> variables,
                                    SqlFunction<PreparedStatement, R> block
                            ) {
                                LOGGER.info("Execute sql : \"{}\", with variables: {}", sql, variables);
                                return DefaultExecutor.INSTANCE.execute(
                                        con,
                                        sql,
                                        variables,
                                        block
                                );
                            }
                        }
                )
                .addScalarProvider(
                        ScalarProvider.enumProviderByString(Gender.class, it ->
                                it
                                        .map(Gender.MALE, "M")
                                        .map(Gender.FEMALE, "F")
                        )
                )
                .build();
    }

    @Bean
    public ApplicationRunner initializeH2DatabaseRunner(SqlClient sqlClient) {
        return args -> {
            sqlClient.getConnectionManager().execute(con -> {
                InputStream inputStream = SqlClient.class
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
        };
    }
}
