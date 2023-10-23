package org.babyfish.jimmer.spring.cfg;

import org.babyfish.jimmer.spring.SpringJSqlClient;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.cache.CacheAbandonedCallback;
import org.babyfish.jimmer.sql.kt.KSqlClient;
import org.babyfish.jimmer.sql.kt.KSqlClientKt;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({TransactionCacheOperatorFlusherConfig.class, MicroServiceExchangeConfig.class})
public class SqlClientConfig {

    @Bean(name = "sqlClient")
    @ConditionalOnMissingBean({JSqlClient.class, KSqlClient.class})
    @ConditionalOnProperty(name = "jimmer.language", havingValue = "java", matchIfMissing = true)
    public JSqlClient javaSqlClient(ApplicationContext ctx, ApplicationEventPublisher publisher) {
        return new SpringJSqlClient(ctx, publisher, false);
    }

    @Bean(name = "sqlClient")
    @ConditionalOnMissingBean({JSqlClient.class, KSqlClient.class})
    @ConditionalOnProperty(name = "jimmer.language", havingValue = "kotlin")
    public KSqlClient kotlinSqlClient(ApplicationContext ctx, ApplicationEventPublisher publisher) {
        return KSqlClientKt.toKSqlClient(
                new SpringJSqlClient(ctx, publisher, true)
        );
    }

    @ConditionalOnMissingBean(CacheAbandonedCallback.class)
    @Bean
    public CacheAbandonedCallback cacheAbandonedCallback() {
        return CacheAbandonedCallback.log();
    }
}
