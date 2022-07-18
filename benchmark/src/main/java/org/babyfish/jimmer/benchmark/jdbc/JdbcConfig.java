package org.babyfish.jimmer.benchmark.jdbc;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.TransactionManager;

import javax.sql.DataSource;

@Configuration
public class JdbcConfig {

    // exposed-spring-boot-starter hides transaction manage of spring-data-jdbc,
    // declare it explicitly and use @Primary
    @Primary
    @Bean
    public TransactionManager transactionManager(DataSource dataSource) {
        return new JdbcTransactionManager(dataSource);
    }
}
