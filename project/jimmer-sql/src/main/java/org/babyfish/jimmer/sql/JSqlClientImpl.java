package org.babyfish.jimmer.sql;

import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.ast.table.Props;
import org.babyfish.jimmer.sql.filter.Filter;
import org.babyfish.jimmer.sql.filter.FilterConfig;
import org.babyfish.jimmer.sql.filter.FilterManager;
import org.babyfish.jimmer.sql.fluent.Fluent;
import org.babyfish.jimmer.sql.fluent.impl.FluentImpl;
import org.babyfish.jimmer.sql.loader.ListLoader;
import org.babyfish.jimmer.sql.loader.ReferenceLoader;
import org.babyfish.jimmer.sql.loader.ValueLoader;
import org.babyfish.jimmer.sql.loader.impl.Loaders;
import org.babyfish.jimmer.sql.association.meta.AssociationType;
import org.babyfish.jimmer.sql.ast.Executable;
import org.babyfish.jimmer.sql.ast.impl.mutation.AssociationsImpl;
import org.babyfish.jimmer.sql.ast.impl.EntitiesImpl;
import org.babyfish.jimmer.sql.ast.impl.mutation.Mutations;
import org.babyfish.jimmer.sql.ast.impl.query.Queries;
import org.babyfish.jimmer.sql.ast.mutation.MutableDelete;
import org.babyfish.jimmer.sql.ast.mutation.MutableUpdate;
import org.babyfish.jimmer.sql.ast.query.ConfigurableRootQuery;
import org.babyfish.jimmer.sql.ast.query.MutableRootQuery;
import org.babyfish.jimmer.sql.ast.table.AssociationTable;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.cache.CacheConfig;
import org.babyfish.jimmer.sql.cache.CacheDisableConfig;
import org.babyfish.jimmer.sql.cache.Caches;
import org.babyfish.jimmer.sql.cache.CachesImpl;
import org.babyfish.jimmer.sql.dialect.DefaultDialect;
import org.babyfish.jimmer.sql.dialect.Dialect;
import org.babyfish.jimmer.sql.event.TriggersImpl;
import org.babyfish.jimmer.sql.meta.IdGenerator;
import org.babyfish.jimmer.sql.runtime.*;

import java.sql.Connection;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

class JSqlClientImpl implements JSqlClient {

    private static final ConnectionManager ILLEGAL_CONNECTION_MANAGER = new ConnectionManager() {
        @Override
        public <R> R execute(Function<Connection, R> block) {
            throw new ExecutionException("ConnectionManager of SqlClient is not configured");
        }
    };

    private final ConnectionManager connectionManager;

    private final ConnectionManager slaveConnectionManager;

    private final Dialect dialect;

    private final Executor executor;

    private final Map<Class<?>, ScalarProvider<?, ?>> scalarProviderMap;

    private final Map<Class<?>, IdGenerator> idGeneratorMap;

    private final int defaultBatchSize;

    private final int defaultListBatchSize;

    private final EntitiesImpl entities;

    private final Caches caches;

    private final Triggers triggers;

    private final TransientResolverManager transientResolverManager;

    private final FilterManager filterManager;

    private final DraftInterceptorManager draftInterceptorManager;

    private JSqlClientImpl(
            ConnectionManager connectionManager,
            ConnectionManager slaveConnectionManager,
            Dialect dialect,
            Executor executor,
            Map<Class<?>, ScalarProvider<?, ?>> scalarProviderMap,
            Map<Class<?>, IdGenerator> idGeneratorMap,
            int defaultBatchSize,
            int defaultListBatchSize,
            EntitiesImpl entities,
            Caches caches,
            Triggers triggers,
            TransientResolverManager transientResolverManager,
            FilterManager filterManager,
            DraftInterceptorManager draftInterceptorManager) {
        this.connectionManager =
                connectionManager != null ?
                        connectionManager :
                        ILLEGAL_CONNECTION_MANAGER;
        this.slaveConnectionManager = slaveConnectionManager;
        this.dialect =
                dialect != null ?
                    dialect :
                    new DefaultDialect();
        this.executor =
                executor != null ?
                        executor :
                        DefaultExecutor.INSTANCE;
        this.scalarProviderMap = scalarProviderMap;
        this.idGeneratorMap = idGeneratorMap;
        this.defaultBatchSize = defaultBatchSize;
        this.defaultListBatchSize = defaultListBatchSize;
        this.entities =
                entities != null ?
                        entities.forSqlClient(this) :
                        new EntitiesImpl(this);
        this.caches =
                caches != null ?
                        caches :
                        CachesImpl.of(triggers, scalarProviderMap, null);
        this.triggers = triggers;
        this.transientResolverManager =
                transientResolverManager != null ?
                        transientResolverManager :
                        createTransientResolverManager();
        this.filterManager = filterManager;
        this.draftInterceptorManager = draftInterceptorManager;
    }

    @Override
    public ConnectionManager getConnectionManager() {
        return connectionManager;
    }

    @Override
    public ConnectionManager getSlaveConnectionManager(boolean forUpdate) {
        ConnectionManager slave = slaveConnectionManager;
        if (slave != null && !forUpdate) {
            return slave;
        }
        return connectionManager;
    }

    @Override
    public Dialect getDialect() {
        return dialect;
    }

    @Override
    public Executor getExecutor() {
        return executor;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T, S> ScalarProvider<T, S> getScalarProvider(Class<T> scalarType) {
        ScalarProvider<T, S> provider = (ScalarProvider<T, S>) scalarProviderMap.get(scalarType);
        return provider != null ?
            provider :
            (ScalarProvider<T, S>)DefaultScalarProviders.getProvider(scalarType);
    }

    @Override
    public IdGenerator getIdGenerator(Class<?> entityType) {
        IdGenerator userIdGenerator = idGeneratorMap.get(entityType);
        if (userIdGenerator == null) {
            userIdGenerator = idGeneratorMap.get(null);
            if (userIdGenerator == null) {
                userIdGenerator = ImmutableType.get(entityType).getIdGenerator();
            }
        }
        return userIdGenerator;
    }

    @Override
    public int getDefaultBatchSize() {
        return defaultBatchSize;
    }

    @Override
    public int getDefaultListBatchSize() {
        return defaultListBatchSize;
    }

    @Override
    public Fluent createFluent() {
        return new FluentImpl(this);
    }

    @Override
    public <T extends Table<?>, R> ConfigurableRootQuery<T, R> createQuery(
            Class<T> tableType,
            BiFunction<MutableRootQuery<T>, T, ConfigurableRootQuery<T, R>> block
    ) {
        return Queries.createQuery(this, tableType, block);
    }

    @Override
    public <SE, ST extends Table<SE>, TE, TT extends Table<TE>, R>
    ConfigurableRootQuery<AssociationTable<SE, ST, TE, TT>, R> createAssociationQuery(
            Class<ST> sourceTableType,
            Function<ST, TT> targetTableGetter,
            BiFunction<
                    MutableRootQuery<AssociationTable<SE, ST, TE, TT>>,
                    AssociationTable<SE, ST, TE, TT>,
                    ConfigurableRootQuery<AssociationTable<SE, ST, TE, TT>, R>
                    > block
    ) {
        return Queries.createAssociationQuery(this, sourceTableType, targetTableGetter, block);
    }

    @Override
    public <T extends Table<?>> Executable<Integer> createUpdate(
            Class<T> tableType,
            BiConsumer<MutableUpdate, T> block
    ) {
        return Mutations.createUpdate(this, tableType, block);
    }

    @Override
    public <T extends Table<?>> Executable<Integer> createDelete(
            Class<T> tableType,
            BiConsumer<MutableDelete, T> block
    ) {
        return Mutations.createDelete(this, tableType, block);
    }

    @Override
    public Entities getEntities() {
        return entities;
    }

    @Override
    public Triggers getTriggers() {
        return triggers;
    }

    @Override
    public <ST extends Table<?>> Associations getAssociations(
            Class<ST> sourceTableType,
            Function<ST, ? extends Table<?>> block
    ) {
        return getAssociations(ImmutableProps.join(sourceTableType, block));
    }

    @Override
    public Associations getAssociations(Class<?> entityType, String prop) {
        return getAssociations(ImmutableType.get(entityType).getProp(prop));
    }

    @Override
    public Associations getAssociations(ImmutableProp immutableProp) {
        return getAssociations(AssociationType.of(immutableProp));
    }

    @Override
    public Associations getAssociations(AssociationType associationType) {
        return new AssociationsImpl(this, null, associationType);
    }

    @Override
    public <S, V> ValueLoader<S, V> getValueLoader(TypedProp.Scalar<S, V> prop) {
        return Loaders.createValueLoader(this, prop.unwrap());
    }

    @Override
    public <SE, ST extends Table<SE>, TE, TT extends Table<TE>> ReferenceLoader<SE, TE, TT>
    getReferenceLoader(Class<ST> sourceTableType, Function<ST, TT> block) {
        return Loaders.createReferenceLoader(
                this,
                ImmutableProps.join(sourceTableType, block)
        );
    }

    @Override
    public <SE, ST extends Table<SE>, TE, TT extends Table<TE>>
    ListLoader<SE, TE, TT> getListLoader(Class<ST> sourceTableType, Function<ST, TT> block) {
        return Loaders.createListLoader(
                this,
                ImmutableProps.join(sourceTableType, block)
        );
    }

    @Override
    public Caches getCaches() {
        return caches;
    }

    @Override
    public JSqlClient caches(Consumer<CacheDisableConfig> block) {
        if (block == null) {
            throw new IllegalArgumentException("block cannot be null");
        }
        CacheDisableConfig cfg = new CacheDisableConfig();
        block.accept(cfg);
        return new JSqlClientImpl(
                connectionManager,
                slaveConnectionManager,
                dialect,
                executor,
                scalarProviderMap,
                idGeneratorMap,
                defaultBatchSize,
                defaultListBatchSize,
                entities,
                new CachesImpl((CachesImpl) caches, cfg),
                triggers,
                transientResolverManager,
                filterManager,
                draftInterceptorManager
        );
    }

    @Override
    public JSqlClient filters(Consumer<FilterConfig> block) {
        if (block == null) {
            throw new IllegalArgumentException("block cannot be null");
        }
        FilterConfig cfg = new FilterConfig(filterManager);
        block.accept(cfg);
        if (cfg.getFilterManager() == filterManager) {
            return this;
        }
        return new JSqlClientImpl(
                connectionManager,
                slaveConnectionManager,
                dialect,
                executor,
                scalarProviderMap,
                idGeneratorMap,
                defaultBatchSize,
                defaultListBatchSize,
                entities,
                caches,
                triggers,
                transientResolverManager,
                cfg.getFilterManager(),
                draftInterceptorManager
        );
    }

    @Override
    public JSqlClient disableSlaveConnectionManager() {
        if (slaveConnectionManager == null) {
            return this;
        }
        return new JSqlClientImpl(
                connectionManager,
                null,
                dialect,
                executor,
                scalarProviderMap,
                idGeneratorMap,
                defaultBatchSize,
                defaultListBatchSize,
                entities,
                caches,
                triggers,
                transientResolverManager,
                filterManager,
                draftInterceptorManager
        );
    }

    @Override
    public TransientResolver<?, ?> getResolver(ImmutableProp prop) {
        return transientResolverManager.get(prop);
    }

    @Override
    public Filter<Props> getFilter(ImmutableType type) {
        return filterManager.get(type);
    }

    @Override
    public Filter<Props> getFilter(ImmutableProp prop) {
        return filterManager.get(prop);
    }

    @Override
    public Filter<Props> getFilter(TypedProp.Association<?, ?> prop) {
        return getFilter(prop.unwrap());
    }

    @Override
    public DraftInterceptor<?> getDraftInterceptor(ImmutableType type) {
        return draftInterceptorManager.get(type);
    }

    private TransientResolverManager createTransientResolverManager() {
        TransientResolverManager manager = new TransientResolverManager(this);
        if (caches != null) { // Important, initialize necessary resolvers
            for (ImmutableType type : ((CachesImpl)caches).getObjectCacheMap().keySet()) {
                for (ImmutableProp prop : type.getProps().values()) {
                    if (prop.hasTransientResolver()) {
                        manager.get(prop);
                    }
                }
            }
            for (ImmutableProp prop : ((CachesImpl)caches).getPropCacheMap().keySet()) {
                if (prop.hasTransientResolver()) {
                    manager.get(prop);
                }
            }
        }
        return manager;
    }

    public static class BuilderImpl implements JSqlClient.Builder {

        private ConnectionManager connectionManager;

        private ConnectionManager slaveConnectionManager;

        private Dialect dialect;

        private Executor executor;

        private final Map<Class<?>, ScalarProvider<?, ?>> scalarProviderMap = new HashMap<>();

        private final Map<Class<?>, IdGenerator> idGeneratorMap = new HashMap<>();

        private int defaultBatchSize = 128;

        private int defaultListBatchSize = 16;

        private Caches caches;

        private final Triggers triggers = new TriggersImpl();

        private final List<Filter<?>> filters = new ArrayList<>();

        private final Set<Filter<?>> disabledFilters = new HashSet<>();

        private final List<DraftInterceptor<?>> interceptors = new ArrayList<>();

        public BuilderImpl() {}

        @Override
        @OldChain
        public JSqlClient.Builder setConnectionManager(ConnectionManager connectionManager) {
            this.connectionManager = connectionManager;
            return this;
        }

        @Override
        @OldChain
        public JSqlClient.Builder setSlaveConnectionManager(ConnectionManager connectionManager) {
            this.slaveConnectionManager = connectionManager;
            return this;
        }

        @Override
        @OldChain
        public JSqlClient.Builder setDialect(Dialect dialect) {
            this.dialect = dialect;
            return this;
        }

        @Override
        @OldChain
        public JSqlClient.Builder setExecutor(Executor executor) {
            this.executor = executor;
            return this;
        }

        @Override
        @OldChain
        public JSqlClient.Builder setIdGenerator(IdGenerator idGenerator) {
            return setIdGenerator(null, idGenerator);
        }

        @Override
        @OldChain
        public JSqlClient.Builder setIdGenerator(Class<?> entityType, IdGenerator idGenerator) {
            idGeneratorMap.put(entityType, idGenerator);
            return this;
        }

        @Override
        @OldChain
        public JSqlClient.Builder addScalarProvider(ScalarProvider<?, ?> scalarProvider) {
            if (scalarProviderMap.containsKey(scalarProvider.getScalarType())) {
                throw new IllegalStateException(
                        "Cannot set scalar provider for scalar type \"" +
                                scalarProvider.getScalarType() +
                                "\" twice"
                );
            }
            scalarProviderMap.put(scalarProvider.getScalarType(), scalarProvider);
            return this;
        }

        @Override
        @OldChain
        public JSqlClient.Builder setDefaultBatchSize(int size) {
            if (size < 1) {
                throw new IllegalStateException("size cannot be less than 1");
            }
            defaultBatchSize = size;
            return this;
        }

        @Override
        @OldChain
        public JSqlClient.Builder setDefaultListBatchSize(int size) {
            if (size < 1) {
                throw new IllegalStateException("size cannot be less than 1");
            }
            defaultListBatchSize = size;
            return this;
        }

        @Override
        @OldChain
        public JSqlClient.Builder setCaches(Consumer<CacheConfig> block) {
            caches = CachesImpl.of(triggers, scalarProviderMap, block);
            return this;
        }

        @Override
        public Builder addFilter(Filter<?> filter) {
            return addFilters(Collections.singletonList(filter));
        }

        @Override
        public Builder addFilters(Filter<?>... filters) {
            return addFilters(Arrays.asList(filters));
        }

        @Override
        public Builder addFilters(Collection<Filter<?>> filters) {
            this.filters.addAll(filters);
            this.disabledFilters.removeAll(filters);
            return this;
        }

        @Override
        public Builder addDisabledFilter(Filter<?> filter) {
            return addDisabledFilters(Collections.singletonList(filter));
        }

        @Override
        public Builder addDisabledFilters(Filter<?>... filters) {
            return addDisabledFilters(Arrays.asList(filters));
        }

        @Override
        public Builder addDisabledFilters(Collection<Filter<?>> filters) {
            this.filters.addAll(filters);
            this.disabledFilters.addAll(filters);
            return this;
        }

        @Override
        public Builder addDraftInterceptor(DraftInterceptor<?> interceptor) {
            return addDraftInterceptors(Collections.singletonList(interceptor));
        }

        @Override
        public Builder addDraftInterceptors(DraftInterceptor<?>... interceptors) {
            return addDraftInterceptors(Arrays.asList(interceptors));
        }

        @Override
        public Builder addDraftInterceptors(Collection<DraftInterceptor<?>> interceptors) {
            this.interceptors.addAll(interceptors);
            return this;
        }

        @Override
        public JSqlClient build() {
            return new JSqlClientImpl(
                    connectionManager,
                    slaveConnectionManager,
                    dialect,
                    executor,
                    scalarProviderMap,
                    idGeneratorMap,
                    defaultBatchSize,
                    defaultListBatchSize,
                    null,
                    caches,
                    triggers,
                    null,
                    new FilterManager(filters, disabledFilters),
                    new DraftInterceptorManager(interceptors));
        }
    }
}

