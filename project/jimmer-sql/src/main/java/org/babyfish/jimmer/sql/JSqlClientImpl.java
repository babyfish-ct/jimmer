package org.babyfish.jimmer.sql;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.ast.impl.mutation.MutableDeleteImpl;
import org.babyfish.jimmer.sql.ast.impl.mutation.MutableUpdateImpl;
import org.babyfish.jimmer.sql.ast.impl.query.MutableRootQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.query.MutableSubQueryImpl;
import org.babyfish.jimmer.sql.ast.query.MutableSubQuery;
import org.babyfish.jimmer.sql.ast.table.TableEx;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;
import org.babyfish.jimmer.sql.event.TriggerType;
import org.babyfish.jimmer.sql.event.Triggers;
import org.babyfish.jimmer.sql.event.TriggersImpl;
import org.babyfish.jimmer.sql.event.binlog.BinLog;
import org.babyfish.jimmer.sql.event.binlog.BinLogParser;
import org.babyfish.jimmer.sql.filter.Filter;
import org.babyfish.jimmer.sql.filter.FilterConfig;
import org.babyfish.jimmer.sql.filter.Filters;
import org.babyfish.jimmer.sql.filter.impl.FilterManager;
import org.babyfish.jimmer.sql.loader.Loaders;
import org.babyfish.jimmer.sql.loader.impl.LoadersImpl;
import org.babyfish.jimmer.sql.association.meta.AssociationType;
import org.babyfish.jimmer.sql.ast.impl.mutation.AssociationsImpl;
import org.babyfish.jimmer.sql.ast.impl.EntitiesImpl;
import org.babyfish.jimmer.sql.ast.mutation.MutableDelete;
import org.babyfish.jimmer.sql.ast.mutation.MutableUpdate;
import org.babyfish.jimmer.sql.ast.query.MutableRootQuery;
import org.babyfish.jimmer.sql.ast.table.AssociationTable;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.cache.CacheConfig;
import org.babyfish.jimmer.sql.cache.CacheDisableConfig;
import org.babyfish.jimmer.sql.cache.Caches;
import org.babyfish.jimmer.sql.cache.CachesImpl;
import org.babyfish.jimmer.sql.dialect.DefaultDialect;
import org.babyfish.jimmer.sql.dialect.Dialect;
import org.babyfish.jimmer.sql.meta.IdGenerator;
import org.babyfish.jimmer.sql.runtime.*;

import java.util.*;
import java.util.function.Consumer;

class JSqlClientImpl implements JSqlClient {

    private final ConnectionManager connectionManager;

    private final ConnectionManager slaveConnectionManager;

    private final Dialect dialect;

    private final Executor executor;

    private final Map<Class<?>, ScalarProvider<?, ?>> scalarProviderMap;

    private final Map<Class<?>, IdGenerator> idGeneratorMap;

    private final int defaultBatchSize;

    private final int defaultListBatchSize;

    private final EntitiesImpl entities;

    private final EntityManager entityManager;

    private final Caches caches;

    private final Triggers triggers;

    private final Triggers transactionTriggers;

    private final BinLog binLog;

    private final TransientResolverManager transientResolverManager;

    private final FilterManager filterManager;

    private final DraftInterceptorManager draftInterceptorManager;

    private final Loaders loaders = new LoadersImpl(this);

    private final ReaderManager readerManager = new ReaderManager(this);

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
            EntityManager entityManager,
            Caches caches,
            Triggers triggers,
            Triggers transactionTriggers,
            BinLog binLog,
            FilterManager filterManager,
            TransientResolverManager transientResolverManager,
            DraftInterceptorManager draftInterceptorManager) {
        this.connectionManager =
                connectionManager != null ?
                        connectionManager :
                        ConnectionManager.ILLEGAL;
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
        this.entityManager = entityManager;
        this.caches =
                caches != null ?
                        caches :
                        CachesImpl.of(triggers, entityManager, null);
        this.triggers = triggers;
        this.transactionTriggers = transactionTriggers;
        this.binLog = binLog;
        this.filterManager = filterManager;
        this.transientResolverManager =
                transientResolverManager != null ?
                        transientResolverManager :
                        createTransientResolverManager();
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
    public <T extends TableProxy<?>> MutableRootQuery<T> createQuery(T table) {
        if (table instanceof TableEx<?>) {
            throw new IllegalArgumentException("Top-level query does not support TableEx");
        }
        return new MutableRootQueryImpl<>(
                this,
                table,
                ExecutionPurpose.QUERY,
                false
        );
    }

    @Override
    public MutableUpdate createUpdate(TableProxy<?> table) {
        return new MutableUpdateImpl(this, table);
    }

    @Override
    public MutableDelete createDelete(TableProxy<?> table) {
        return new MutableDeleteImpl(this, table);
    }

    @Override
    public <SE, ST extends Table<SE>, TE, TT extends Table<TE>>
    MutableRootQuery<AssociationTable<SE, ST, TE, TT>> createAssociationQuery(
            AssociationTable<SE, ST, TE, TT> table
    ) {
        if (!(table instanceof TableProxy<?>)) {
            throw new IllegalArgumentException("The argument \"table\" must be proxy");
        }
        return new MutableRootQueryImpl<>(
                this,
                (TableProxy<?>) table,
                ExecutionPurpose.QUERY,
                false
        );
    }

    @Override
    public MutableSubQuery createSubQuery(TableProxy<?> table) {
        return new MutableSubQueryImpl(this, table);
    }

    @Override
    public <SE, ST extends TableEx<SE>, TE, TT extends TableEx<TE>>
    MutableSubQuery createAssociationSubQuery(AssociationTable<SE, ST, TE, TT> table) {
        if (!(table instanceof TableProxy<?>)) {
            throw new IllegalArgumentException("The argument \"table\" must be proxy");
        }
        return new MutableSubQueryImpl(this, (TableProxy<?>) table);
    }

    @Override
    public Entities getEntities() {
        return entities;
    }

    @Override
    public TriggerType getTriggerType() {
        if (transactionTriggers == null) {
            return TriggerType.BINLOG_ONLY;
        }
        if (transactionTriggers == triggers) {
            return TriggerType.TRANSACTION_ONLY;
        }
        return TriggerType.BOTH;
    }

    @Override
    public Triggers getTriggers() {
        return triggers;
    }

    @Override
    public Triggers getTriggers(boolean transaction) {
        if (transaction) {
            Triggers tt = this.transactionTriggers;
            if (tt == null) {
                throw new IllegalStateException("Transaction triggers is not supported by current sql client");
            }
            return tt;
        }
        return triggers;
    }

    public Triggers tryGetTransactionTriggers() {
        return transactionTriggers;
    }

    @Override
    public BinLog getBinLog() {
        BinLog bl = binLog;
        if (bl == null) {
            throw new IllegalStateException("binLog is not supported because the entityManager of sql client is not specified");
        }
        return bl;
    }

    @Override
    public Associations getAssociations(TypedProp.Association<?, ?> prop) {
        return getAssociations(prop.unwrap());
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
    public Loaders getLoaders() {
        return loaders;
    }

    @Override
    public EntityManager getEntityManager() {
        return entityManager;
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
                entityManager,
                new CachesImpl((CachesImpl) caches, cfg),
                triggers,
                transactionTriggers,
                binLog,
                filterManager,
                transientResolverManager,
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
                entityManager,
                caches,
                triggers,
                transactionTriggers,
                binLog,
                cfg.getFilterManager(),
                transientResolverManager,
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
                entityManager,
                caches,
                triggers,
                transactionTriggers,
                binLog,
                filterManager,
                transientResolverManager,
                draftInterceptorManager
        );
    }

    @Override
    public TransientResolver<?, ?> getResolver(ImmutableProp prop) {
        return transientResolverManager.get(prop);
    }

    @Override
    public Filters getFilters() {
        return filterManager;
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

    @Override
    public Reader<?> getReader(Class<?> type) {
        return readerManager.reader(type);
    }

    @Override
    public Reader<?> getReader(ImmutableType type) {
        return readerManager.reader(type);
    }

    @Override
    public Reader<?> getReader(ImmutableProp prop) {
        return readerManager.reader(prop);
    }

    public static class BuilderImpl implements JSqlClient.Builder {

        private ConnectionManager connectionManager;

        private ConnectionManager slaveConnectionManager;

        private Dialect dialect;

        private Executor executor;

        private final Map<Class<?>, ScalarProvider<?, ?>> scalarProviderMap = new HashMap<>();

        private final Map<Class<?>, IdGenerator> idGeneratorMap = new HashMap<>();

        private int defaultBatchSize = DEFAULT_BATCH_SIZE;

        private int defaultListBatchSize = DEFAULT_LIST_BATCH_SIZE;

        private EntityManager entityManager;

        private Caches caches;

        private TriggerType triggerType = TriggerType.BINLOG_ONLY;

        private Triggers triggers;

        private Triggers transactionTriggers;

        private final List<Filter<?>> filters = new ArrayList<>();

        private final Set<Filter<?>> disabledFilters = new HashSet<>();

        private final List<DraftInterceptor<?>> interceptors = new ArrayList<>();

        private ObjectMapper binLogObjectMapper;

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
            Class<?> scalarType = scalarProvider.getScalarType();
            if (scalarProviderMap.containsKey(scalarType)) {
                throw new IllegalStateException(
                        "Cannot set scalar provider for scalar type \"" +
                                scalarType +
                                "\" twice"
                );
            }
            if (Collection.class.isAssignableFrom(scalarType) || Map.class.isAssignableFrom(scalarType)) {
                throw new IllegalStateException(
                        "Illegal scalar provider type \"" +
                                scalarProvider.getClass() +
                                "\" is illegal, the scalar type \"" +
                                scalarType +
                                "\" cannot be collection or map"
                );
            }
            Class<?> ormAnnoType = getOrmAnnotationType(scalarType);
            if (ormAnnoType != null) {
                throw new IllegalStateException(
                        "Illegal scalar provider type \"" +
                                scalarProvider.getClass() +
                                "\" is illegal, the scalar type \"" +
                                scalarType +
                                "\" or it super types cannot be decorated by \"@" +
                                ormAnnoType.getName() +
                                "\""
                );
            }
            scalarProviderMap.put(scalarType, scalarProvider);
            return this;
        }

        private Class<?> getOrmAnnotationType(Class<?> type) {
            if (type == null) {
                return null;
            }
            if (type != Object.class) {
                if (type.isAnnotationPresent(Entity.class)) {
                    return Entity.class;
                }
                if (type.isAssignableFrom(MappedSuperclass.class)) {
                    return MappedSuperclass.class;
                }
                if (type.isAssignableFrom(Embeddable.class)) {
                    return Embeddable.class;
                }
            }
            Class<?> annoType = getOrmAnnotationType(type.getSuperclass());
            if (annoType != null) {
                return annoType;
            }
            for (Class<?> interfaceType : type.getInterfaces()) {
                annoType = getOrmAnnotationType(interfaceType);
                if (annoType != null) {
                    return annoType;
                }
            }
            return null;
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
        public Builder setEntityManager(EntityManager scanner) {
            if (entityManager != null) {
                throw new IllegalStateException(
                        "The EntityManager of SqlBuilder.Builder can only be set once"
                );
            }
            entityManager = scanner;
            return this;
        }

        @Override
        @OldChain
        public JSqlClient.Builder setCaches(Consumer<CacheConfig> block) {
            if (caches != null) {
                throw new IllegalStateException("caches cannot be set twice");
            }
            createTriggersIfNecessary();
            caches = CachesImpl.of(triggers, entityManager, block);
            return this;
        }

        @Override
        public Builder setTriggerType(TriggerType triggerType) {
            this.triggerType = triggerType != null ? triggerType : TriggerType.BINLOG_ONLY;
            return this;
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
        public Builder setBinLogObjectMapper(ObjectMapper mapper) {
            this.binLogObjectMapper = mapper;
            return this;
        }

        @Override
        public JSqlClient build() {
            createTriggersIfNecessary();
            FilterManager filterManager = new FilterManager(filters, disabledFilters);
            BinLogParser binLogParser = new BinLogParser();
            BinLog binLog;
            if (entityManager != null) {
                binLog = new BinLog(
                        entityManager,
                        binLogParser,
                        triggers
                );
            } else {
                binLog = null;
            }
            JSqlClient sqlClient = new JSqlClientImpl(
                    connectionManager,
                    slaveConnectionManager,
                    dialect,
                    executor,
                    scalarProviderMap,
                    idGeneratorMap,
                    defaultBatchSize,
                    defaultListBatchSize,
                    null,
                    entityManager,
                    caches,
                    triggers,
                    transactionTriggers,
                    binLog,
                    filterManager,
                    null,
                    new DraftInterceptorManager(interceptors)
            );
            filterManager.initialize(sqlClient);
            binLogParser.initialize(sqlClient, binLogObjectMapper);
            return sqlClient;
        }

        private void createTriggersIfNecessary() {
            if (triggers == null) {
                switch (triggerType) {
                    case TRANSACTION_ONLY:
                        transactionTriggers = triggers = new TriggersImpl();
                        break;
                    case BOTH:
                        triggers = new TriggersImpl();
                        transactionTriggers = new TriggersImpl();
                        break;
                    default:
                        triggers = new TriggersImpl();
                        break;
                }
            }
        }
    }
}

