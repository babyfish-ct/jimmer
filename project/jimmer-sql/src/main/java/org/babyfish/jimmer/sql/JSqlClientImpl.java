package org.babyfish.jimmer.sql;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TargetLevel;
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
import org.babyfish.jimmer.sql.filter.BuiltInFilters;
import org.babyfish.jimmer.sql.filter.Filter;
import org.babyfish.jimmer.sql.filter.FilterConfig;
import org.babyfish.jimmer.sql.filter.Filters;
import org.babyfish.jimmer.sql.filter.impl.BuiltinFiltersImpl;
import org.babyfish.jimmer.sql.filter.impl.FilterManager;
import org.babyfish.jimmer.sql.loader.graphql.Loaders;
import org.babyfish.jimmer.sql.loader.graphql.impl.LoadersImpl;
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

import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Consumer;

class JSqlClientImpl implements JSqlClient {

    private final ConnectionManager connectionManager;

    private final ConnectionManager slaveConnectionManager;

    private final Dialect dialect;

    private final Executor executor;

    private final Map<Class<?>, ScalarProvider<?, ?>> scalarProviderMap;

    private final Map<ImmutableProp, ScalarProvider<?, ?>> propScalarProviderMap;

    private final Map<Class<?>, IdGenerator> idGeneratorMap;

    private final DefaultScalarProvider defaultScalarProvider;

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
            Map<ImmutableProp, ScalarProvider<?, ?>> propScalarProviderMap,
            Map<Class<?>, IdGenerator> idGeneratorMap,
            DefaultScalarProvider defaultScalarProvider,
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
        this.propScalarProviderMap = propScalarProviderMap;
        this.idGeneratorMap = idGeneratorMap;
        this.defaultScalarProvider = defaultScalarProvider;
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
        this.transientResolverManager = transientResolverManager;
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
            (ScalarProvider<T, S>) defaultScalarProvider.getProvider(scalarType);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T, S> ScalarProvider<T, S> getScalarProvider(ImmutableProp prop) {
        ScalarProvider<T, S> provider = (ScalarProvider<T, S>) propScalarProviderMap.get(prop);
        if (provider != null) {
            return provider;
        }
        if (prop.isScalar(TargetLevel.ENTITY)) {
            return getScalarProvider((Class<T>)prop.getElementClass());
        }
        return null;
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
                propScalarProviderMap,
                idGeneratorMap,
                defaultScalarProvider,
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
                propScalarProviderMap,
                idGeneratorMap,
                defaultScalarProvider,
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
                propScalarProviderMap,
                idGeneratorMap,
                defaultScalarProvider,
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
    public Class<? extends TransientResolverProvider> getResolverProviderClass() {
        return transientResolverManager.getProviderClass();
    }

    @Override
    public Filters getFilters() {
        return filterManager;
    }

    @Override
    public DraftInterceptor<?> getDraftInterceptor(ImmutableType type) {
        return draftInterceptorManager.get(type);
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

        private TransientResolverProvider transientResolverProvider;

        private final Map<Class<?>, ScalarProvider<?, ?>> scalarProviderMap = new HashMap<>();

        private final Map<ImmutableProp, ScalarProvider<?, ?>> propScalarProviderMap = new HashMap<>();

        private final Map<Class<?>, IdGenerator> idGeneratorMap = new HashMap<>();

        private EnumType.Strategy defaultEnumStrategy = EnumType.Strategy.NAME;

        private int defaultBatchSize = DEFAULT_BATCH_SIZE;

        private int defaultListBatchSize = DEFAULT_LIST_BATCH_SIZE;

        private EntityManager entityManager;

        private Caches caches;

        private TriggerType triggerType = TriggerType.BINLOG_ONLY;

        private Triggers triggers;

        private Triggers transactionTriggers;

        private final List<Filter<?>> filters = new ArrayList<>();

        private final Set<Filter<?>> disabledFilters = new HashSet<>();

        private boolean ignoreBuiltInFilters = false;

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
        public JSqlClient.Builder setTransientResolverProvider(TransientResolverProvider transientResolverProvider) {
            this.transientResolverProvider = transientResolverProvider;
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
            Collection<ImmutableProp> props = scalarProvider.getHandledProps();
            if (props == null || props.isEmpty()) {
                addScalarProviderImpl(null, scalarProvider);
            } else {
                for (ImmutableProp prop : props) {
                    if (prop == null) {
                        throw new IllegalStateException(
                                "Each property of returned list of \"" +
                                        scalarProvider.getClass().getName() +
                                        ".getHandledProps" +
                                        "\" cannot be null"
                        );
                    }
                    if (!prop.isScalar(TargetLevel.ENTITY) && !prop.isScalarList()) {
                        throw new IllegalStateException(
                                "Each property of returned list of \"" +
                                        scalarProvider.getClass().getName() +
                                        ".getHandledProps" +
                                        "\" must be scalar, but \"" +
                                        prop +
                                        "\" is not"
                        );
                    }
                    addScalarProviderImpl(prop, scalarProvider);
                }
            }
            return this;
        }

        @Override
        public Builder addScalarProvider(TypedProp<?, ?> prop, ScalarProvider<?, ?> scalarProvider) {
            if (prop == null) {
                throw new IllegalArgumentException("prop cannot be null");
            }
            addScalarProviderImpl(prop.unwrap(), scalarProvider);
            return this;
        }

        @Override
        public Builder addScalarProvider(ImmutableProp prop, ScalarProvider<?, ?> scalarProvider) {
            if (prop == null) {
                throw new IllegalArgumentException("prop cannot be null");
            }
            addScalarProviderImpl(prop, scalarProvider);
            return this;
        }

        private void addScalarProviderImpl(ImmutableProp prop, ScalarProvider<?, ?> scalarProvider) {
            Type scalarType = scalarProvider.getScalarType();
            if (prop == null) {
                if (!(scalarType instanceof Class<?>)) {
                    throw new IllegalStateException(
                            "Illegal scalar provider type \"" +
                                    scalarProvider.getClass().getName() +
                                    "\" its scalar type argument cannot be \"" +
                                    scalarType +
                                    "\" because it is global scalar provider, " +
                                    "please use property-specific scalar provider"
                    );
                }
                if (scalarProviderMap.containsKey(scalarType)) {
                    throw new IllegalStateException(
                            "Cannot set scalar provider for scalar type \"" +
                                    scalarType +
                                    "\" twice"
                    );
                }
                if (((Class<?>) scalarType).isArray() ||
                        Iterable.class.isAssignableFrom((Class<?>) scalarType) ||
                        Map.class.isAssignableFrom((Class<?>) scalarType)) {
                    throw new IllegalStateException(
                            "Illegal scalar provider type \"" +
                                    scalarProvider.getClass().getName() +
                                    "\" its scalar type argument cannot be array, collection or map," +
                                    " because it is global scalar provider, " +
                                    "please use property-specific scalar provider"
                    );
                }
                scalarProviderMap.put((Class<?>) scalarType, scalarProvider);
            } else {
                if (!prop.isScalar(TargetLevel.ENTITY) && !prop.isScalarList()) {
                    throw new IllegalStateException(
                            "Cannot set scalar provider for property type \"" +
                                    prop +
                                    "\" because the property is not scalar property"
                    );
                }
                if (propScalarProviderMap.containsKey(prop)) {
                    throw new IllegalStateException(
                            "Cannot set scalar provider for property type \"" +
                                    prop +
                                    "\" twice"
                    );
                }
                propScalarProviderMap.put(prop, scalarProvider);
            }
        }

        @Override
        public Builder setDefaultEnumStrategy(EnumType.Strategy strategy) {
            this.defaultEnumStrategy = strategy != null ? strategy : EnumType.Strategy.NAME;
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
        public Builder setEntityManager(EntityManager entityManager) {
            if (this.entityManager != null && this.entityManager != entityManager) {
                throw new IllegalStateException(
                        "The EntityManager of SqlBuilder.Builder can only be set once"
                );
            }
            this.entityManager = entityManager;
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
            for (Filter<?> filter : filters) {
                if (filter != null) {
                    this.filters.add(filter);
                }
            }
            return this;
        }

        @Override
        public Builder addDisabledFilters(Filter<?>... filters) {
            return addDisabledFilters(Arrays.asList(filters));
        }

        @Override
        public Builder addDisabledFilters(Collection<Filter<?>> filters) {
            for (Filter<?> filter : filters) {
                if (filter != null) {
                    this.filters.add(filter);
                    this.disabledFilters.add(filter);
                }
            }
            return this;
        }

        @Override
        public Builder ignoreBuiltInFilters() {
            ignoreBuiltInFilters = true;
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
            for (DraftInterceptor<?> interceptor : interceptors) {
                if (interceptor != null) {
                    this.interceptors.add(interceptor);
                }
            }
            return this;
        }

        @Override
        public Builder setBinLogObjectMapper(ObjectMapper mapper) {
            this.binLogObjectMapper = mapper;
            return this;
        }

        @Override
        public JSqlClient build() {
            if (entityManager == null) {
                throw new IllegalStateException("The `entityManager` of SqlClient has not been configured");
            }
            createTriggersIfNecessary();
            FilterManager filterManager = createFilterManager();
            BinLogParser binLogParser = new BinLogParser();
            BinLog binLog = new BinLog(
                    entityManager,
                    binLogParser,
                    triggers
            );
            TransientResolverManager transientResolverManager =
                    new TransientResolverManager(
                            transientResolverProvider != null ?
                                    transientResolverProvider :
                                    DefaultTransientResolverProvider.INSTANCE
                    );
            JSqlClient sqlClient = new JSqlClientImpl(
                    connectionManager,
                    slaveConnectionManager,
                    dialect,
                    executor,
                    scalarProviderMap,
                    propScalarProviderMap,
                    idGeneratorMap,
                    new DefaultScalarProvider(defaultEnumStrategy),
                    defaultBatchSize,
                    defaultListBatchSize,
                    null,
                    entityManager,
                    caches,
                    triggers,
                    transactionTriggers,
                    binLog,
                    filterManager,
                    transientResolverManager,
                    new DraftInterceptorManager(interceptors)
            );
            filterManager.initialize(sqlClient);
            binLogParser.initialize(sqlClient, binLogObjectMapper);
            transientResolverManager.initialize(sqlClient);
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

        private FilterManager createFilterManager() {
            BuiltInFilters builtInFilters = new BuiltinFiltersImpl();
            if (ignoreBuiltInFilters) {
                return new FilterManager(builtInFilters, filters, disabledFilters);
            }
            List<Filter<?>> mergedFilters = new ArrayList<>(filters);
            List<Filter<?>> mergedDisabledFilters = new ArrayList<>(disabledFilters);
            for (ImmutableType type : entityManager.getAllTypes()) {
                Filter<?> notDeletedFilter = builtInFilters.getDeclaredNotDeletedFilter(type);
                Filter<?> alreadyDeletedFilter = builtInFilters.getDeclaredAlreadyDeletedFilter(type);
                if (notDeletedFilter != null) {
                    mergedFilters.add(notDeletedFilter);
                }
                if (alreadyDeletedFilter != null) {
                    mergedFilters.add(alreadyDeletedFilter);
                    mergedDisabledFilters.add(alreadyDeletedFilter);
                }
            }
            return new FilterManager(builtInFilters, mergedFilters, mergedDisabledFilters);
        }
    }
}

