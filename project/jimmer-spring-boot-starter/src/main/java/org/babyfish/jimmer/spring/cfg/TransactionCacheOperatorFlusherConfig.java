package org.babyfish.jimmer.spring.cfg;

import org.babyfish.jimmer.spring.cache.impl.TransactionCacheOperatorFlusher;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.cache.CacheOperator;
import org.babyfish.jimmer.sql.cache.TransactionCacheOperator;
import org.babyfish.jimmer.sql.kt.KSqlClient;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@EnableScheduling
@Configuration
public class TransactionCacheOperatorFlusherConfig {

    @Bean
    public TransactionCacheOperatorFlusher transactionCacheOperatorFlusher(
            List<JSqlClient> jSqlClients,
            List<KSqlClient> kSqlClients
    ) {
        Set<TransactionCacheOperator> transactionCacheOperators = new LinkedHashSet<>();
        for (JSqlClient sqlClient : jSqlClients) {
            CacheOperator op = ((JSqlClientImplementor)sqlClient).getCacheOperator();
            if (op instanceof TransactionCacheOperator) {
                transactionCacheOperators.add((TransactionCacheOperator) op);
            }
        }
        for (KSqlClient sqlClient : kSqlClients) {
            CacheOperator op = sqlClient.getJavaClient().getCacheOperator();
            if (op instanceof TransactionCacheOperator) {
                transactionCacheOperators.add((TransactionCacheOperator) op);
            }
        }
        if (transactionCacheOperators.isEmpty()) {
            return null;
        }
        return new TransactionCacheOperatorFlusher(
                new ArrayList<>(transactionCacheOperators)
        );
    }
}
