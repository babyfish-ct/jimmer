package org.babyfish.jimmer.spring.cfg;

import org.babyfish.jimmer.spring.cache.impl.TransactionCacheOperatorFlusher;
import org.babyfish.jimmer.sql.cache.TransactionCacheOperator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.List;

@ConditionalOnBean(TransactionCacheOperator.class)
@EnableScheduling
@Configuration
public class TransactionCacheOperatorFlusherConfig {

    @Bean
    public TransactionCacheOperatorFlusher transactionCacheOperatorFlusher(
            List<TransactionCacheOperator> transactionCacheOperators
    ) {
        return new TransactionCacheOperatorFlusher(transactionCacheOperators);
    }
}
