package org.babyfish.jimmer.sql.example.graphql.cfg;

import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.example.graphql.entities.Gender;
import org.babyfish.jimmer.sql.runtime.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.function.Function;

@Configuration
public class SqlClientConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqlClientConfig.class);

    @Bean
    public SqlClient sqlClient(DataSource dataSource) {
        return SqlClient.newBuilder()
                .setConnectionManager(
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
                .setExecutor(
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


}
