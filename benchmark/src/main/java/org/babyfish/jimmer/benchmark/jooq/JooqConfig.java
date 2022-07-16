package org.babyfish.jimmer.benchmark.jooq;

import org.jooq.DSLContext;
import org.jooq.impl.DataSourceConnectionProvider;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.DefaultDSLContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class JooqConfig {

    private final DataSource dataSource;

    public JooqConfig(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Bean
    public DSLContext dslContext() {
        DefaultConfiguration jooqConfiguration = new DefaultConfiguration();
        jooqConfiguration.set(
                // Need not wrap it by "TransactionAwareDataSourceProxy"
                // Let it run as fast as possible.
                new DataSourceConnectionProvider(dataSource)
        );
        return new DefaultDSLContext(jooqConfiguration);
    }
}
