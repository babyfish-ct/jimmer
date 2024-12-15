package org.babyfish.jimmer.spring;

import org.babyfish.jimmer.spring.cfg.JimmerProperties;
import org.babyfish.jimmer.spring.cfg.support.SpringConnectionManager;
import org.babyfish.jimmer.spring.cfg.support.SpringLogicalDeletedValueGeneratorProvider;
import org.babyfish.jimmer.spring.cfg.support.SpringTransientResolverProvider;
import org.babyfish.jimmer.spring.cfg.support.SpringUserIdGeneratorProvider;
import org.babyfish.jimmer.spring.dialect.DialectDetector;
import org.babyfish.jimmer.spring.meta.SpringMetaStringResolver;
import org.babyfish.jimmer.spring.util.ApplicationContextUtils;
import org.babyfish.jimmer.sql.DraftInterceptor;
import org.babyfish.jimmer.sql.DraftPreProcessor;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.cache.CacheAbandonedCallback;
import org.babyfish.jimmer.sql.cache.CacheFactory;
import org.babyfish.jimmer.sql.cache.CacheOperator;
import org.babyfish.jimmer.sql.di.*;
import org.babyfish.jimmer.sql.dialect.DefaultDialect;
import org.babyfish.jimmer.sql.dialect.Dialect;
import org.babyfish.jimmer.sql.event.TriggerType;
import org.babyfish.jimmer.sql.event.Triggers;
import org.babyfish.jimmer.sql.filter.Filter;
import org.babyfish.jimmer.sql.kt.cfg.KCustomizer;
import org.babyfish.jimmer.sql.kt.cfg.KCustomizerKt;
import org.babyfish.jimmer.sql.kt.cfg.KInitializer;
import org.babyfish.jimmer.sql.kt.cfg.KInitializerKt;
import org.babyfish.jimmer.sql.kt.filter.KFilter;
import org.babyfish.jimmer.sql.kt.filter.impl.JavaFiltersKt;
import org.babyfish.jimmer.sql.meta.DatabaseNamingStrategy;
import org.babyfish.jimmer.sql.meta.MetaStringResolver;
import org.babyfish.jimmer.sql.runtime.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.EmbeddedValueResolver;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ConfigurableApplicationContext;

import javax.sql.DataSource;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.babyfish.jimmer.impl.util.ObjectUtil.firstNonNullOf;
import static org.babyfish.jimmer.impl.util.ObjectUtil.optionalFirstNonNullOf;

class JSpringSqlClient extends JLazyInitializationSqlClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(JSpringSqlClient.class);

    private final ApplicationContext ctx;

    private final DataSource dataSource;

    private final Consumer<JSqlClient.Builder> block;

    private final boolean isKotlin;

    public JSpringSqlClient(
            ApplicationContext ctx,
            DataSource dataSource,
            Consumer<JSqlClient.Builder> block,
            boolean isKotlin
    ) {
        this.ctx = Objects.requireNonNull(ctx, "ctx cannot be null");
        this.dataSource = dataSource;
        this.block = block;
        this.isKotlin = isKotlin;
    }

    @Override
    protected JSqlClient.Builder createBuilder() {

        boolean isCfgKotlin = "kotlin".equalsIgnoreCase(getRequiredBean(JimmerProperties.class).getLanguage());
        if (isCfgKotlin != isKotlin) {
            throw new IllegalStateException(
                    "Cannot create sql client for \"" +
                    (isKotlin ? "kotlin" : "java") +
                    "\" because \"jimmer.language\" in \"application.properties/application.yml\" is \"" +
                    (isCfgKotlin ? "kotlin" : "java") +
                    "\""
            );
        }

        JimmerProperties properties = getRequiredBean(JimmerProperties.class);
        UserIdGeneratorProvider userIdGeneratorProvider = getOptionalBean(UserIdGeneratorProvider.class);
        LogicalDeletedValueGeneratorProvider logicalDeletedValueGeneratorProvider = getOptionalBean(LogicalDeletedValueGeneratorProvider.class);
        TransientResolverProvider transientResolverProvider = getOptionalBean(TransientResolverProvider.class);
        AopProxyProvider aopProxyProvider = getOptionalBean(AopProxyProvider.class);
        EntityManager entityManager = getOptionalBean(EntityManager.class);
        DatabaseNamingStrategy databaseNamingStrategy = getOptionalBean(DatabaseNamingStrategy.class);
        MetaStringResolver metaStringResolver = getOptionalBean(MetaStringResolver.class);
        Dialect dialect = getOptionalBean(Dialect.class);
        DialectDetector dialectDetector = getOptionalBean(DialectDetector.class);
        Executor executor = getOptionalBean(Executor.class);
        SqlFormatter sqlFormatter = getOptionalBean(SqlFormatter.class);
        CacheFactory cacheFactory = getOptionalBean(CacheFactory.class);
        CacheOperator cacheOperator = getOptionalBean(CacheOperator.class);
        MicroServiceExchange exchange = getOptionalBean(MicroServiceExchange.class);
        Collection<CacheAbandonedCallback> callbacks = getObjects(CacheAbandonedCallback.class);
        Collection<ScalarProvider<?, ?>> providers = getObjects(ScalarProvider.class);
        Collection<DraftPreProcessor<?>> processors = getObjects(DraftPreProcessor.class);
        Collection<DraftInterceptor<?, ?>> interceptors = getObjects(DraftInterceptor.class);
        Collection<ExceptionTranslator<?>> exceptionTranslators = getObjects(ExceptionTranslator.class);

        JSqlClient.Builder builder = JSqlClient.newBuilder();
        if (userIdGeneratorProvider != null) {
            builder.setUserIdGeneratorProvider(userIdGeneratorProvider);
        } else {
            builder.setUserIdGeneratorProvider(new SpringUserIdGeneratorProvider(ctx));
        }
        if (logicalDeletedValueGeneratorProvider != null) {
            builder.setLogicalDeletedValueGeneratorProvider(logicalDeletedValueGeneratorProvider);
        } else {
            builder.setLogicalDeletedValueGeneratorProvider(new SpringLogicalDeletedValueGeneratorProvider(ctx));
        }
        if (transientResolverProvider != null) {
            builder.setTransientResolverProvider(transientResolverProvider);
        } else {
            builder.setTransientResolverProvider(new SpringTransientResolverProvider(ctx));
        }
        if (aopProxyProvider != null) {
            builder.setAopProxyProvider(aopProxyProvider);
        } else {
            builder.setAopProxyProvider(AopUtils::getTargetClass);
        }
        if (entityManager != null) {
            builder.setEntityManager(entityManager);
        }
        if (databaseNamingStrategy != null) {
            builder.setDatabaseNamingStrategy(databaseNamingStrategy);
        }
        if (metaStringResolver != null) {
            builder.setMetaStringResolver(metaStringResolver);
        } else if (ctx instanceof ConfigurableApplicationContext) {
            ConfigurableListableBeanFactory beanFactory = ((ConfigurableApplicationContext) ctx).getBeanFactory();
            builder.setMetaStringResolver(new SpringMetaStringResolver(new EmbeddedValueResolver(beanFactory)));
        }

        builder.setTriggerType(properties.getTriggerType());
        builder.setDefaultDissociateActionCheckable(properties.isDefaultDissociationActionCheckable());
        builder.setIdOnlyTargetCheckingLevel(properties.getIdOnlyTargetCheckingLevel());
        builder.setDefaultEnumStrategy(properties.getDefaultEnumStrategy());
        builder.setDefaultBatchSize(properties.getDefaultBatchSize());
        builder.setDefaultListBatchSize(properties.getDefaultListBatchSize());
        builder.setInListPaddingEnabled(properties.isInListPaddingEnabled());
        builder.setExpandedInListPaddingEnabled(properties.isExpandedInListPaddingEnabled());
        builder.setOffsetOptimizingThreshold(properties.getOffsetOptimizingThreshold());
        builder.setForeignKeyEnabledByDefault(properties.isForeignKeyEnabledByDefault());
        builder.setMaxCommandJoinCount(properties.getMaxCommandJoinDepth());
        builder.setTargetTransferable(properties.isTargetTransferable());
        builder.setExecutorContextPrefixes(properties.getExecutorContextPrefixes());
        if (properties.isShowSql()) {
            builder.setExecutor(Executor.log(executor));
        } else {
            builder.setExecutor(executor);
        }
        if (sqlFormatter != null) {
            builder.setSqlFormatter(sqlFormatter);
        } else if (properties.isPrettySql()) {
            if (properties.isInlineSqlVariables()) {
                builder.setSqlFormatter(SqlFormatter.INLINE_PRETTY);
            } else {
                builder.setSqlFormatter(SqlFormatter.PRETTY);
            }
        }
        builder
                .setDatabaseValidationMode(properties.getDatabaseValidation().getMode())
                .setDatabaseValidationCatalog(properties.getDatabaseValidation().getCatalog())
                .setDatabaseValidationSchema(properties.getDatabaseValidation().getSchema())
                .setCacheFactory(cacheFactory)
                .setCacheOperator(cacheOperator)
                .addCacheAbandonedCallbacks(callbacks);

        for (ScalarProvider<?, ?> provider : providers) {
            builder.addScalarProvider(provider);
        }

        builder.addDraftPreProcessors(processors);
        builder.addDraftInterceptors(interceptors);
        builder.addExceptionTranslators(exceptionTranslators);
        initializeByLanguage(builder);
        builder.addInitializers(new SpringEventInitializer(ctx));

        builder.setMicroServiceName(properties.getMicroServiceName());
        if (!properties.getMicroServiceName().isEmpty()) {
            builder.setMicroServiceExchange(exchange);
        }

        if (block != null) {
            block.accept(builder);
        }

        ConnectionManager connectionManager = firstNonNullOf(
                () -> ((JSqlClientImplementor.Builder) builder).getConnectionManager(),
                () -> getOptionalBean(ConnectionManager.class),
                () -> dataSource == null ? null : new SpringConnectionManager(dataSource),
                () -> new SpringConnectionManager(getRequiredBean(DataSource.class))
        );

        builder.setConnectionManager(connectionManager);

        if (((JSqlClientImplementor.Builder) builder).getDialect().getClass() == DefaultDialect.class) {
            DialectDetector finalDetector = dialectDetector != null ?
                    dialectDetector :
                    DialectDetector.INSTANCE;
            builder.setDialect(
                    optionalFirstNonNullOf(
                            () -> dialect,
                            properties::getDialect,
                            () -> connectionManager.execute(finalDetector::detectDialect)
                    )
            );
        }

        return builder;
    }

    private void initializeByLanguage(JSqlClient.Builder builder) {

        Collection<Filter<?>> javaFilters = getObjects(Filter.class);
        Collection<Customizer> javaCustomizers = getObjects(Customizer.class);
        Collection<Initializer> javaInitializers = getObjects(Initializer.class);
        Collection<KFilter<?>> kotlinFilters = getObjects(KFilter.class);
        Collection<KCustomizer> kotlinCustomizers = getObjects(KCustomizer.class);
        Collection<KInitializer> kotlinInitializers = getObjects(KInitializer.class);

        if (isKotlin) {
            if (!javaFilters.isEmpty()) {
                LOGGER.warn(
                        "Jimmer is working in kotlin mode, but some java filters " +
                        "has been found in spring context, they will be ignored"
                );
            }
            if (!javaCustomizers.isEmpty()) {
                LOGGER.warn(
                        "Jimmer is working in kotlin mode, but some java customizers " +
                        "has been found in spring context, they will be ignored"
                );
            }
            if (!javaInitializers.isEmpty()) {
                LOGGER.warn(
                        "Jimmer is working in kotlin mode, but some java initializers " +
                        "has been found in spring context, they will be ignored"
                );
            }
            builder.addFilters(
                    kotlinFilters
                            .stream()
                            .map(JavaFiltersKt::toJavaFilter)
                            .collect(Collectors.toList())
            );
            builder.addCustomizers(
                    kotlinCustomizers
                            .stream()
                            .map(KCustomizerKt::toJavaCustomizer)
                            .collect(Collectors.toList())
            );
            builder.addInitializers(
                    kotlinInitializers
                            .stream()
                            .map(KInitializerKt::toJavaInitializer)
                            .collect(Collectors.toList())
            );
        } else {
            if (!kotlinFilters.isEmpty()) {
                LOGGER.warn(
                        "Jimmer is working in java mode, but some kotlin filters " +
                        "has been found in spring context, they will be ignored"
                );
            }
            if (!kotlinCustomizers.isEmpty()) {
                LOGGER.warn(
                        "Jimmer is working in java mode, but some kotlin customizers " +
                        "has been found in spring context, they will be ignored"
                );
            }
            if (!kotlinInitializers.isEmpty()) {
                LOGGER.warn(
                        "Jimmer is working in kotlin mode, but some kotlin initializers " +
                        "has been found in spring context, they will be ignored"
                );
            }
            builder.addFilters(javaFilters);
            builder.addCustomizers(javaCustomizers);
            builder.addInitializers(javaInitializers);
        }
    }

    private <T> T getRequiredBean(Class<T> type) {
        return ctx.getBean(type);
    }

    private <T> T getOptionalBean(Class<T> type) {
        try {
            return ctx.getBean(type);
        } catch (NoSuchBeanDefinitionException ex) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private <T> Collection<T> getObjects(Class<?> beanType) {
        return (Collection<T>) ApplicationContextUtils.getBeansOfType(ctx, beanType);
    }

    private static class SpringEventInitializer implements Initializer {

        private final ApplicationEventPublisher publisher;

        private SpringEventInitializer(ApplicationEventPublisher publisher) {
            this.publisher = publisher;
        }

        @Override
        public void initialize(JSqlClient sqlClient) throws Exception {
            Triggers[] triggersArr = ((JSqlClientImplementor) sqlClient).getTriggerType() == TriggerType.BOTH ?
                    new Triggers[]{sqlClient.getTriggers(), sqlClient.getTriggers(true)} :
                    new Triggers[]{sqlClient.getTriggers()};
            for (Triggers triggers : triggersArr) {
                triggers.addEntityListener(publisher::publishEvent);
                triggers.addAssociationListener(publisher::publishEvent);
            }
        }
    }
}
