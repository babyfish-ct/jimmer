package org.babyfish.jimmer.spring.cfg;

import kotlin.Unit;
import org.babyfish.jimmer.spring.repository.SpringConnectionManager;
import org.babyfish.jimmer.sql.DraftInterceptor;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.cache.CacheFactory;
import org.babyfish.jimmer.sql.dialect.Dialect;
import org.babyfish.jimmer.sql.filter.Filter;
import org.babyfish.jimmer.sql.kt.KSqlClient;
import org.babyfish.jimmer.sql.kt.KSqlClientKt;
import org.babyfish.jimmer.sql.kt.filter.KFilter;
import org.babyfish.jimmer.sql.kt.filter.impl.JavaFiltersKt;
import org.babyfish.jimmer.sql.runtime.EntityManager;
import org.babyfish.jimmer.sql.runtime.Executor;
import org.babyfish.jimmer.sql.runtime.ScalarProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class SqlClientConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqlClientConfig.class);

    @Bean(name = "sqlClient")
    @ConditionalOnMissingBean({JSqlClient.class, KSqlClient.class})
    @ConditionalOnProperty(name = "jimmer.language", havingValue = "java", matchIfMissing = true)
    public JSqlClient javaSqlClient(
            JimmerProperties properties,
            @Autowired(required = false) DataSource dataSource,
            @Autowired(required = false) SpringConnectionManager connectionManager,
            @Autowired(required = false) EntityManager entityManager,
            @Autowired(required = false) Dialect dialect,
            @Autowired(required = false) Executor executor,
            @Autowired(required = false) CacheFactory cacheFactory,
            List<ScalarProvider<?, ?>> providers,
            List<DraftInterceptor<?>> interceptors,
            List<Filter<?>> filters,
            List<KFilter<?>> kotlinFilters,
            List<JimmerCustomizer> customizers,
            List<JimmerInitializer> initializers
    ) {
        if (!kotlinFilters.isEmpty()) {
            LOGGER.warn(
                    "Jimmer is working in java mode, but some kotlin filters " +
                            "has been found in spring context, they will be ignored"
            );
        }
        JSqlClient.Builder builder = JSqlClient.newBuilder();
        preCreateSqlClient(
                builder,
                properties,
                dataSource,
                connectionManager,
                entityManager,
                dialect,
                executor,
                cacheFactory,
                providers,
                interceptors,
                filters,
                customizers
        );
        JSqlClient sqlClient = builder.build();
        postCreateSqlClient(sqlClient, initializers);
        return sqlClient;
    }

    @Bean(name = "sqlClient")
    @ConditionalOnMissingBean({JSqlClient.class, KSqlClient.class})
    @ConditionalOnProperty(name = "jimmer.language", havingValue = "kotlin")
    public KSqlClient kotlinSqlClient(
            JimmerProperties properties,
            @Autowired(required = false) DataSource dataSource,
            @Autowired(required = false) SpringConnectionManager connectionManager,
            @Autowired(required = false) EntityManager entityManager,
            @Autowired(required = false) Dialect dialect,
            @Autowired(required = false) Executor executor,
            @Autowired(required = false) CacheFactory cacheFactory,
            List<ScalarProvider<?, ?>> providers,
            List<DraftInterceptor<?>> interceptors,
            List<Filter<?>> javaFilters,
            List<KFilter<?>> filters,
            List<JimmerCustomizer> customizers,
            List<JimmerInitializer> initializers
    ) {
        if (!javaFilters.isEmpty()) {
            LOGGER.warn(
                    "Jimmer is working in kotlin mode, but some java filters " +
                            "has been found in spring context, they will be ignored"
            );
        }
        KSqlClient sqlClient = KSqlClientKt.newKSqlClient(dsl -> {
            preCreateSqlClient(
                    dsl.getJavaBuilder(),
                    properties,
                    dataSource,
                    connectionManager,
                    entityManager,
                    dialect,
                    executor,
                    cacheFactory,
                    providers,
                    interceptors,
                    filters
                            .stream()
                            .map(JavaFiltersKt::toJavaFilter)
                            .collect(Collectors.toList()),
                    customizers
            );
            return Unit.INSTANCE;
        });
        postCreateSqlClient(sqlClient.getJavaClient(), initializers);
        return sqlClient;
    }

    private static void preCreateSqlClient(
            JSqlClient.Builder builder,
            JimmerProperties properties,
            DataSource dataSource,
            SpringConnectionManager connectionManager,
            EntityManager entityManager,
            Dialect dialect,
            Executor executor,
            CacheFactory cacheFactory,
            List<ScalarProvider<?, ?>> providers,
            List<DraftInterceptor<?>> interceptors,
            List<Filter<?>> filters,
            List<JimmerCustomizer> customizers
    ) {
        if (connectionManager != null) {
            builder.setConnectionManager(connectionManager);
        } else if (dataSource != null) {
            builder.setConnectionManager(new SpringConnectionManager(dataSource));
        }

        if (entityManager != null) {
            builder.setEntityManager(entityManager);
        }

        builder.setDialect(dialect != null ? dialect : properties.getDialect());
        builder.setDefaultBatchSize(properties.getDefaultBatchSize());
        builder.setDefaultListBatchSize(properties.getDefaultListBatchSize());
        if (properties.isShowSql()) {
            builder.setExecutor(Executor.log(executor));
        } else {
            builder.setExecutor(executor);
        }

        if (cacheFactory != null) {
            builder.setCaches(cfg -> {
                cfg.setCacheFactory(cacheFactory);
            });
        }

        for (ScalarProvider<?, ?> provider : providers) {
            builder.addScalarProvider(provider);
        }

        builder.addDraftInterceptors(interceptors);

        builder.addFilters(filters);

        for (JimmerCustomizer customizer : customizers) {
            customizer.customize(builder);
        }
    }

    private static void postCreateSqlClient(
            JSqlClient sqlClient,
            List<JimmerInitializer> initializers
    ) {
        if (!(sqlClient.getConnectionManager() instanceof SpringConnectionManager)) {
            throw new IllegalStateException(
                    "The connection manager of sqlClient must be \"" +
                            SpringConnectionManager.class.getName() +
                            "\""
            );
        }

        if (sqlClient.getSlaveConnectionManager(false) != null &&
                !(sqlClient.getSlaveConnectionManager(false) instanceof SpringConnectionManager)) {
            throw new IllegalStateException(
                    "The slave connection manager of sqlClient must be null or \"" +
                            SpringConnectionManager.class.getName() +
                            "\""
            );
        }

        for (JimmerInitializer initializer : initializers) {
            initializer.initialize(sqlClient);
        }
    }
}
