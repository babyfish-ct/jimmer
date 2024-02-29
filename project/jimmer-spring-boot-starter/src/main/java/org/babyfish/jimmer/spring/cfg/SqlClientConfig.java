package org.babyfish.jimmer.spring.cfg;

import org.babyfish.jimmer.spring.SqlClients;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.cache.CacheAbandonedCallback;
import org.babyfish.jimmer.sql.kt.KSqlClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.List;

@Configuration
@Import({TransactionCacheOperatorFlusherConfig.class, MicroServiceExchangeConfig.class})
public class SqlClientConfig {

    @Bean(name = "sqlClient")
    @ConditionalOnMissingBean({JSqlClient.class, KSqlClient.class})
    @ConditionalOnProperty(name = "jimmer.language", havingValue = "java", matchIfMissing = true)
    public JSqlClient javaSqlClient(ApplicationContext ctx) {
        return SqlClients.java(ctx);
    }

    @Bean(name = "sqlClient")
    @ConditionalOnMissingBean({JSqlClient.class, KSqlClient.class})
    @ConditionalOnProperty(name = "jimmer.language", havingValue = "kotlin")
    public KSqlClient kotlinSqlClient(ApplicationContext ctx) {
        return SqlClients.kotlin(ctx);
    }

    @Bean
    public SqlClientInitializer sqlClientInitializer(
            List<JSqlClient> javaSqlClients,
            List<KSqlClient> kotlinSqlClients
    ) {
        return new SqlClientInitializer(javaSqlClients, kotlinSqlClients);
    }

    @ConditionalOnMissingBean(CacheAbandonedCallback.class)
    @Bean
    public CacheAbandonedCallback cacheAbandonedCallback() {
        return CacheAbandonedCallback.log();
    }
}
