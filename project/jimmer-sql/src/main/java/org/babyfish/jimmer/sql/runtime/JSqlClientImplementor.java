package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.DraftInterceptor;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.TransientResolver;
import org.babyfish.jimmer.sql.cache.CacheDisableConfig;
import org.babyfish.jimmer.sql.dialect.Dialect;
import org.babyfish.jimmer.sql.event.TriggerType;
import org.babyfish.jimmer.sql.filter.FilterConfig;
import org.babyfish.jimmer.sql.meta.IdGenerator;
import org.babyfish.jimmer.sql.meta.MetadataStrategy;

import java.util.List;
import java.util.function.Consumer;

public interface JSqlClientImplementor extends JSqlClient {

    ConnectionManager getConnectionManager();

    ConnectionManager getSlaveConnectionManager(boolean forUpdate);

    Dialect getDialect();

    Executor getExecutor();

    EntityManager getEntityManager();

    MetadataStrategy getMetadataStrategy();

    List<String> getExecutorContextPrefixes();

    <T, S> ScalarProvider<T, S> getScalarProvider(Class<T> scalarType);

    <T, S> ScalarProvider<T, S> getScalarProvider(TypedProp<T, ?> prop);

    <T, S> ScalarProvider<T, S> getScalarProvider(ImmutableProp prop);

    IdGenerator getIdGenerator(Class<?> entityType);

    int getDefaultBatchSize();

    int getDefaultListBatchSize();

    int getOffsetOptimizingThreshold();

    TriggerType getTriggerType();

    TransientResolver<?, ?> getResolver(ImmutableProp prop);

    Class<? extends TransientResolverProvider> getResolverProviderClass();

    DraftInterceptor<?> getDraftInterceptor(ImmutableType type);

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
}
