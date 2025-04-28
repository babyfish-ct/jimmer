package org.babyfish.jimmer.spring.cfg;

import org.babyfish.jimmer.spring.SqlClients;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.cache.CacheAbandonedCallback;
import org.babyfish.jimmer.sql.kt.KSqlClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
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
    @ConditionalOnExpression("'java'.equalsIgnoreCase(environment.getProperty('jimmer.language')) || !environment.containsProperty('jimmer.language')")
    public JSqlClient javaSqlClient(ApplicationContext ctx) {
        return SqlClients.java(ctx);
    }

    @Bean(name = "sqlClient")
    @ConditionalOnMissingBean({JSqlClient.class, KSqlClient.class})
    @ConditionalOnExpression("'kotlin'.equalsIgnoreCase(environment.getProperty('jimmer.language'))")
    public KSqlClient kotlinSqlClient(ApplicationContext ctx) {
        return SqlClients.kotlin(ctx);
    }

    @ConditionalOnMissingBean(CacheAbandonedCallback.class)
    @Bean
    public CacheAbandonedCallback cacheAbandonedCallback() {
        return CacheAbandonedCallback.log();
    }
}
