package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.DraftInterceptor;
import org.babyfish.jimmer.sql.DraftPreProcessor;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.TransientResolver;
import org.babyfish.jimmer.sql.cache.CacheDisableConfig;
import org.babyfish.jimmer.sql.cache.CacheOperator;
import org.babyfish.jimmer.sql.di.StrategyProvider;
import org.babyfish.jimmer.sql.dialect.Dialect;
import org.babyfish.jimmer.sql.event.TriggerType;
import org.babyfish.jimmer.sql.fetcher.ReferenceFetchType;
import org.babyfish.jimmer.sql.filter.FilterConfig;
import org.babyfish.jimmer.sql.loader.graphql.Loaders;
import org.babyfish.jimmer.sql.meta.IdGenerator;
import org.babyfish.jimmer.sql.meta.MetadataStrategy;
import org.babyfish.jimmer.sql.meta.SqlContext;
import org.babyfish.jimmer.sql.meta.UserIdGenerator;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.time.ZoneId;
import java.util.List;
import java.util.function.Consumer;

public interface JSqlClientImplementor extends JSqlClient, SqlContext {

    ConnectionManager getConnectionManager();

    ConnectionManager getSlaveConnectionManager(boolean forUpdate);

    Dialect getDialect();

    Executor getExecutor();

    EntityManager getEntityManager();

    MetadataStrategy getMetadataStrategy();

    List<String> getExecutorContextPrefixes();

    SqlFormatter getSqlFormatter();

    CacheOperator getCacheOperator();

    TriggerType getTriggerType();

    <T, S> ScalarProvider<T, S> getScalarProvider(Class<T> scalarType);

    <T, S> ScalarProvider<T, S> getScalarProvider(TypedProp<T, ?> prop);

    <T, S> ScalarProvider<T, S> getScalarProvider(ImmutableProp prop);

    ReferenceFetchType getDefaultReferenceFetchType();

    int getMaxJoinFetchDepth();

    ZoneId getZoneId();

    IdGenerator getIdGenerator(Class<?> entityType);

    int getDefaultBatchSize();

    int getDefaultListBatchSize();

    boolean isInListPaddingEnabled();

    boolean isExpandedInListPaddingEnabled();

    int getOffsetOptimizingThreshold();

    int getMaxCommandJoinCount();

    boolean isTargetTransferable();

    boolean isBatchForbidden(boolean dumbBatchAcceptable);

    boolean isUpsertWithUniqueConstraintSupported(ImmutableType type);

    boolean isConstraintViolationTranslatable();

    boolean isMutationTransactionRequired();

    @Nullable
    ExceptionTranslator<Exception> getExceptionTranslator();

    TransientResolver<?, ?> getResolver(ImmutableProp prop);

    StrategyProvider<UserIdGenerator<?>> getUserIdGeneratorProvider();

    StrategyProvider<TransientResolver<?, ?>> getTransientResolverProvider();

    boolean isDefaultDissociationActionCheckable();

    IdOnlyTargetCheckingLevel getIdOnlyTargetCheckingLevel();

    DraftPreProcessor<?> getDraftPreProcessor(ImmutableType type);

    @Nullable
    DraftInterceptor<?, ?> getDraftInterceptor(ImmutableType type);

    Reader<?> getReader(Class<?> type);

    Reader<?> getReader(ImmutableType type);

    Reader<?> getReader(ImmutableProp prop);

    String getMicroServiceName();

    MicroServiceExchange getMicroServiceExchange();

    @Override
    JSqlClientImplementor caches(Consumer<CacheDisableConfig> block);

    @Override
    JSqlClientImplementor filters(Consumer<FilterConfig> block);

    @Override
    JSqlClientImplementor disableSlaveConnectionManager();

    @Override
    JSqlClientImplementor executor(Executor executor);

    Loaders getLoaders();

    interface Builder extends JSqlClient.Builder {

        ConnectionManager getConnectionManager();

        Dialect getDialect();
    }
}
