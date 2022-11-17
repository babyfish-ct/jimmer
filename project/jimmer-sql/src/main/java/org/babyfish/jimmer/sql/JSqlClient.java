package org.babyfish.jimmer.sql;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.babyfish.jimmer.lang.NewChain;
import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.association.meta.AssociationType;
import org.babyfish.jimmer.sql.ast.query.*;
import org.babyfish.jimmer.sql.ast.table.AssociationTable;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;
import org.babyfish.jimmer.sql.cache.CacheConfig;
import org.babyfish.jimmer.sql.cache.CacheDisableConfig;
import org.babyfish.jimmer.sql.cache.Caches;
import org.babyfish.jimmer.sql.event.TriggerType;
import org.babyfish.jimmer.sql.event.Triggers;
import org.babyfish.jimmer.sql.event.binlog.BinLog;
import org.babyfish.jimmer.sql.filter.Filter;
import org.babyfish.jimmer.sql.filter.FilterConfig;
import org.babyfish.jimmer.sql.filter.Filters;
import org.babyfish.jimmer.sql.loader.Loaders;
import org.babyfish.jimmer.sql.meta.IdGenerator;
import org.babyfish.jimmer.sql.ast.mutation.MutableDelete;
import org.babyfish.jimmer.sql.ast.mutation.MutableUpdate;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.dialect.Dialect;
import org.babyfish.jimmer.sql.runtime.*;

import java.util.Collection;
import java.util.function.Consumer;

public interface JSqlClient extends SubQueryProvider {

    static Builder newBuilder() {
        return new JSqlClientImpl.BuilderImpl();
    }

    ConnectionManager getConnectionManager();

    ConnectionManager getSlaveConnectionManager(boolean forUpdate);

    Dialect getDialect();

    Executor getExecutor();

    <T, S> ScalarProvider<T, S> getScalarProvider(Class<T> scalarType);

    IdGenerator getIdGenerator(Class<?> entityType);

    int getDefaultBatchSize();

    int getDefaultListBatchSize();

    <T extends TableProxy<?>> MutableRootQuery<T> createQuery(T table);

    MutableUpdate createUpdate(TableProxy<?> table);

    MutableDelete createDelete(TableProxy<?> table);

    <SE, ST extends Table<SE>, TE, TT extends Table<TE>>
    MutableRootQuery<AssociationTable<SE, ST, TE, TT>> createAssociationQuery(
            AssociationTable<SE,ST, TE, TT> table
    );

    Entities getEntities();

    TriggerType getTriggerType();

    Triggers getTriggers();

    Triggers getTriggers(boolean transaction);

    BinLog getBinLog();

    Associations getAssociations(TypedProp.Association<?, ?> prop);

    Associations getAssociations(ImmutableProp immutableProp);

    Associations getAssociations(AssociationType associationType);

    Loaders getLoaders();

    EntityManager getEntityManager();

    Caches getCaches();

    @NewChain
    JSqlClient caches(Consumer<CacheDisableConfig> block);

    @NewChain
    JSqlClient filters(Consumer<FilterConfig> block);

    @NewChain
    JSqlClient disableSlaveConnectionManager();

    TransientResolver<?, ?> getResolver(ImmutableProp prop);

    Filters getFilters();

    DraftInterceptor<?> getDraftInterceptor(ImmutableType type);

    Reader<?> getReader(Class<?> type);

    Reader<?> getReader(ImmutableType type);

    Reader<?> getReader(ImmutableProp prop);

    interface Builder {

        @OldChain
        Builder setConnectionManager(ConnectionManager connectionManager);

        @OldChain
        Builder setSlaveConnectionManager(ConnectionManager connectionManager);

        @OldChain
        Builder setDialect(Dialect dialect);

        @OldChain
        Builder setExecutor(Executor executor);

        @OldChain
        Builder setIdGenerator(IdGenerator idGenerator);

        @OldChain
        Builder setIdGenerator(Class<?> entityType, IdGenerator idGenerator);

        @OldChain
        Builder addScalarProvider(ScalarProvider<?, ?> scalarProvider);

        @OldChain
        Builder setDefaultBatchSize(int size);

        @OldChain
        Builder setDefaultListBatchSize(int size);

        @OldChain
        Builder setEntityManager(EntityManager scanner);

        @OldChain
        Builder setCaches(Consumer<CacheConfig> block);

        @OldChain
        Builder setTriggerType(TriggerType triggerType);

        @OldChain
        Builder addFilters(Filter<?>... filters);

        @OldChain
        Builder addFilters(Collection<Filter<?>> filters);

        @OldChain
        Builder addDisabledFilters(Filter<?>... filters);

        @OldChain
        Builder addDisabledFilters(Collection<Filter<?>> filters);

        @OldChain
        Builder addDraftInterceptor(DraftInterceptor<?> interceptor);

        @OldChain
        Builder addDraftInterceptors(DraftInterceptor<?>... interceptors);

        @OldChain
        Builder addDraftInterceptors(Collection<DraftInterceptor<?>> interceptors);

        @OldChain
        Builder setBinLogObjectMapper(ObjectMapper mapper);

        JSqlClient build();
    }
}
