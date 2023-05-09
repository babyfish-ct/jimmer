package org.babyfish.jimmer.sql;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.meta.*;
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
import org.babyfish.jimmer.sql.meta.*;
import org.babyfish.jimmer.sql.runtime.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Consumer;

class JSqlClientImpl implements JSqlClientImplementor {

    private final ConnectionManager connectionManager;

    private final ConnectionManager slaveConnectionManager;

    private final Dialect dialect;

    private final Executor executor;

    private final List<String> executorContextPrefixes;

    private final SqlFormatter sqlFormatter;

    private final Map<Class<?>, IdGenerator> idGeneratorMap;

    private final ScalarProviderManager scalarProviderManager;

    private final int defaultBatchSize;

    private final int defaultListBatchSize;

    private final int offsetOptimizingThreshold;

    private final EntitiesImpl entities;

    private final EntityManager entityManager;

    private final Caches caches;

    private final Triggers triggers;

    private final Triggers transactionTriggers;

    private final MetadataStrategy metadataStrategy;

    private final BinLog binLog;

    private final TransientResolverManager transientResolverManager;

    private final FilterManager filterManager;

    private final DraftInterceptorManager draftInterceptorManager;

    private final String microServiceName;

    private final MicroServiceExchange microServiceExchange;

    private final Loaders loaders = new LoadersImpl(this);

    private final ReaderManager readerManager = new ReaderManager(this);

    private JSqlClientImpl(
            ConnectionManager connectionManager,
            ConnectionManager slaveConnectionManager,
            Dialect dialect,
            Executor executor,
            List<String> executorContextPrefixes,
            SqlFormatter sqlFormatter,
            Map<Class<?>, IdGenerator> idGeneratorMap,
            ScalarProviderManager scalarProviderManager,
            int defaultBatchSize,
            int defaultListBatchSize,
            int offsetOptimizingThreshold,
            EntitiesImpl entities,
            EntityManager entityManager,
            Caches caches,
            Triggers triggers,
            Triggers transactionTriggers,
            MetadataStrategy metadataStrategy,
            BinLog binLog,
            FilterManager filterManager,
            TransientResolverManager transientResolverManager,
            DraftInterceptorManager draftInterceptorManager,
            String microServiceName,
            MicroServiceExchange microServiceExchange
    ) {
        this.connectionManager =
                connectionManager != null ?
                        connectionManager :
                        ConnectionManager.ILLEGAL;
        this.slaveConnectionManager = slaveConnectionManager;
        this.dialect = dialect;
        this.executor =
                executor != null ?
                        executor :
                        DefaultExecutor.INSTANCE;
        this.executorContextPrefixes =
                executorContextPrefixes != null ?
                        Collections.unmodifiableList(executorContextPrefixes) :
                        null;
        this.sqlFormatter = sqlFormatter;
        this.idGeneratorMap = idGeneratorMap;
        this.scalarProviderManager = scalarProviderManager;
        this.defaultBatchSize = defaultBatchSize;
        this.defaultListBatchSize = defaultListBatchSize;
        this.offsetOptimizingThreshold = offsetOptimizingThreshold;
        this.entities =
                entities != null ?
                        entities.forSqlClient(this) :
                        new EntitiesImpl(this);
        this.entityManager = entityManager;
        this.caches =
                caches != null ?
                        caches :
                        CachesImpl.of(triggers, entityManager, microServiceName, null);
        this.triggers = triggers;
        this.transactionTriggers = transactionTriggers;
        this.metadataStrategy = metadataStrategy;
        this.binLog = binLog;
        this.filterManager = filterManager;
        this.transientResolverManager = transientResolverManager;
        this.draftInterceptorManager = draftInterceptorManager;
        this.microServiceName = microServiceName;
        this.microServiceExchange = microServiceExchange;
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

    @Override
    public List<String> getExecutorContextPrefixes() {
        return executorContextPrefixes;
    }

    @Override
    public SqlFormatter getSqlFormatter() {
        return sqlFormatter;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T, S> ScalarProvider<T, S> getScalarProvider(Class<T> scalarType) {
        return (ScalarProvider<T, S>) scalarProviderManager.getProvider(scalarType);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T, S> ScalarProvider<T, S> getScalarProvider(TypedProp<T, ?> prop) {
        return (ScalarProvider<T, S>) scalarProviderManager.getProvider(prop.unwrap());
    }

    @SuppressWarnings("unchecked")
    public <T, S> ScalarProvider<T, S> getScalarProvider(ImmutableProp prop) {
        return (ScalarProvider<T, S>) scalarProviderManager.getProvider(prop);
    }

    @Override
    public IdGenerator getIdGenerator(Class<?> entityType) {
        IdGenerator userIdGenerator = idGeneratorMap.get(entityType);
        if (userIdGenerator == null) {
            userIdGenerator = idGeneratorMap.get(null);
            if (userIdGenerator == null) {
                userIdGenerator = ImmutableType.get(entityType).getIdGenerator(metadataStrategy);
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
    public int getOffsetOptimizingThreshold() {
        return offsetOptimizingThreshold;
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

    @Override
    public MetadataStrategy getMetadataStrategy() {
        return metadataStrategy;
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
    public JSqlClientImplementor caches(Consumer<CacheDisableConfig> block) {
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
                executorContextPrefixes,
                sqlFormatter,
                idGeneratorMap,
                scalarProviderManager,
                defaultBatchSize,
                defaultListBatchSize,
                offsetOptimizingThreshold,
                entities,
                entityManager,
                new CachesImpl((CachesImpl) caches, cfg),
                triggers,
                transactionTriggers,
                metadataStrategy,
                binLog,
                filterManager,
                transientResolverManager,
                draftInterceptorManager,
                microServiceName,
                microServiceExchange
        );
    }

    @Override
    public JSqlClientImplementor filters(Consumer<FilterConfig> block) {
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
                executorContextPrefixes,
                sqlFormatter,
                idGeneratorMap,
                scalarProviderManager,
                defaultBatchSize,
                defaultListBatchSize,
                offsetOptimizingThreshold,
                entities,
                entityManager,
                caches,
                triggers,
                transactionTriggers,
                metadataStrategy,
                binLog,
                cfg.getFilterManager(),
                transientResolverManager,
                draftInterceptorManager,
                microServiceName,
                microServiceExchange
        );
    }

    @Override
    public JSqlClientImplementor disableSlaveConnectionManager() {
        if (slaveConnectionManager == null) {
            return this;
        }
        return new JSqlClientImpl(
                connectionManager,
                null,
                dialect,
                executor,
                executorContextPrefixes,
                sqlFormatter,
                idGeneratorMap,
                scalarProviderManager,
                defaultBatchSize,
                defaultListBatchSize,
                offsetOptimizingThreshold,
                entities,
                entityManager,
                caches,
                triggers,
                transactionTriggers,
                metadataStrategy,
                binLog,
                filterManager,
                transientResolverManager,
                draftInterceptorManager,
                microServiceName,
                microServiceExchange
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

    @Override
    public String getMicroServiceName() {
        return microServiceName;
    }

    @Override
    public MicroServiceExchange getMicroServiceExchange() {
        return microServiceExchange;
    }

    public static class BuilderImpl implements JSqlClient.Builder {

        private static final Logger LOGGER = LoggerFactory.getLogger(BuilderImpl.class);

        private ConnectionManager connectionManager;

        private ConnectionManager slaveConnectionManager;

        private Dialect dialect = DefaultDialect.INSTANCE;

        private Executor executor;

        private List<String> executorContextPrefixes;

        private SqlFormatter sqlFormatter = SqlFormatter.SIMPLE;

        private TransientResolverProvider transientResolverProvider;

        private final Map<Class<?>, ScalarProvider<?, ?>> typeScalarProviderMap = new HashMap<>();

        private final Map<ImmutableProp, ScalarProvider<?, ?>> propScalarProviderMap = new HashMap<>();

        private final Map<Class<?>, IdGenerator> idGeneratorMap = new HashMap<>();

        private EnumType.Strategy defaultEnumStrategy = EnumType.Strategy.NAME;

        private DatabaseNamingStrategy databaseNamingStrategy = DefaultDatabaseNamingStrategy.UPPER_CASE;

        private int defaultBatchSize = DEFAULT_BATCH_SIZE;

        private int defaultListBatchSize = DEFAULT_LIST_BATCH_SIZE;

        private int offsetOptimizingThreshold = Integer.MAX_VALUE;

        private EntityManager userEntityManager;

        private EntityManager defaultEntityManager;

        private Caches caches;

        private TriggerType triggerType = TriggerType.BINLOG_ONLY;

        private Triggers triggers;

        private Triggers transactionTriggers;

        private final List<Filter<?>> filters = new ArrayList<>();

        private final Set<Filter<?>> disabledFilters = new HashSet<>();

        private boolean ignoreBuiltInFilters = false;

        private final List<DraftInterceptor<?>> interceptors = new ArrayList<>();

        private ObjectMapper binLogObjectMapper;

        private boolean isForeignKeyEnabledByDefault = true;

        private final Set<Customizer> customizers = new LinkedHashSet<>();

        private final Set<Initializer> initializers = new LinkedHashSet<>();

        private DatabaseValidationMode databaseValidationMode = DatabaseValidationMode.NONE;

        private String databaseValidationCatalog;

        private String microServiceName = "";

        private MicroServiceExchange microServiceExchange;

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
            this.dialect = dialect != null ? dialect : DefaultDialect.INSTANCE;
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
        public Builder setExecutorContextPrefixes(Collection<String> prefixes) {
            if (prefixes == null || prefixes.isEmpty()) {
                executorContextPrefixes = null;
            } else {
                Set<String> set = new TreeSet<>();
                for (String prefix : prefixes) {
                    if (prefix != null && !prefix.isEmpty()) {
                        set.add(prefix);
                    }
                }
                if (set.isEmpty()) {
                    executorContextPrefixes = null;
                } else {
                    executorContextPrefixes = new ArrayList<>(set);
                }
            }
            return this;
        }

        @Override
        public Builder setSqlFormatter(SqlFormatter sqlFormatter) {
            this.sqlFormatter = sqlFormatter != null ? sqlFormatter : SqlFormatter.SIMPLE;
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
                if (typeScalarProviderMap.containsKey(scalarType)) {
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
                typeScalarProviderMap.put((Class<?>) scalarType, scalarProvider);
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
                ImmutableProp originalProp = prop.toOriginal();
                if (originalProp != prop) {
                    throw new IllegalArgumentException(
                            "\"" +
                                    prop +
                                    "\" hides \"" +
                                    originalProp +
                                    "\", please add scalar provider for that hidden property"
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
        public Builder setDatabaseNamingStrategy(DatabaseNamingStrategy strategy) {
            this.databaseNamingStrategy = strategy != null ? strategy : DefaultDatabaseNamingStrategy.UPPER_CASE;
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
        public Builder setOffsetOptimizingThreshold(int threshold) {
            if (threshold <= 0) {
                throw new IllegalArgumentException("`threshold` must be greater than 0");
            }
            offsetOptimizingThreshold = threshold;
            return this;
        }

        @Override
        @OldChain
        public Builder setEntityManager(EntityManager entityManager) {
            if (this.userEntityManager != null && this.userEntityManager != entityManager) {
                throw new IllegalStateException(
                        "The EntityManager of SqlBuilder.Builder can only be set once"
                );
            }
            if (caches != null) {
                throw new IllegalStateException(
                        "The EntityManager cannot be changed after caches is set"
                );
            }
            this.userEntityManager = entityManager;
            return this;
        }

        @Override
        @OldChain
        public JSqlClient.Builder setCaches(Consumer<CacheConfig> block) {
            if (caches != null) {
                throw new IllegalStateException("caches cannot be set twice");
            }
            createTriggersIfNecessary();
            caches = CachesImpl.of(triggers, entityManager(), microServiceName, block);
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
        public Builder setForeignKeyEnabledByDefault(boolean enabled) {
            this.isForeignKeyEnabledByDefault = enabled;
            return this;
        }

        @Override
        public Builder addCustomizers(Customizer... customizers) {
            for (Customizer customizer : customizers) {
                if (customizer != null) {
                    this.customizers.add(customizer);
                }
            }
            return this;
        }

        @Override
        public Builder addCustomizers(Collection<Customizer> customizers) {
            for (Customizer customizer : customizers) {
                if (customizer != null) {
                    this.customizers.add(customizer);
                }
            }
            return this;
        }

        @Override
        public Builder addInitializers(Initializer ... initializers) {
            for (Initializer initializer : initializers) {
                if (initializer != null) {
                    this.initializers.add(initializer);
                }
            }
            return this;
        }

        @Override
        public Builder addInitializers(Collection<Initializer> initializers) {
            for (Initializer initializer : initializers) {
                if (initializer != null) {
                    this.initializers.add(initializer);
                }
            }
            return this;
        }

        @Override
        public Builder setDatabaseValidationMode(DatabaseValidationMode databaseValidationMode) {
            this.databaseValidationMode = Objects.requireNonNull(
                    databaseValidationMode,
                    "argument cannot be null"
            );
            return this;
        }

        @Override
        public Builder setDatabaseValidationCatalog(String catalog) {
            this.databaseValidationCatalog = catalog != null && !catalog.isEmpty() ? catalog : null;
            return this;
        }

        @Override
        public Builder setMicroServiceName(String microServiceName) {
            this.microServiceName = microServiceName != null ? microServiceName : "";
            return this;
        }

        @Override
        public Builder setMicroServiceExchange(MicroServiceExchange exchange) {
            this.microServiceExchange = exchange;
            return this;
        }

        @Override
        public JSqlClient build() {
            for (Customizer customizer : customizers) {
                try {
                    customizer.customize(this);
                } catch (Exception ex) {
                    throw new ExecutionException(
                            "Failed to execute customizer before create sql client",
                            ex
                    );
                }
            }
            if (!microServiceName.isEmpty() && microServiceExchange == null) {
                throw new IllegalStateException(
                        "The `microServiceExchange` must be configured when `microServiceName` is configured"
                );
            }
            FilterManager filterManager = createFilterManager();
            validateAssociations(filterManager);
            createTriggersIfNecessary();
            ForeignKeyStrategy foreignKeyStrategy;
            if (!dialect.isForeignKeySupported()) {
                foreignKeyStrategy = ForeignKeyStrategy.FORCED_FAKE;
            } else if (isForeignKeyEnabledByDefault) {
                foreignKeyStrategy = ForeignKeyStrategy.REAL;
            } else {
                foreignKeyStrategy = ForeignKeyStrategy.FAKE;
            }
            MetadataStrategy metadataStrategy =
                    new MetadataStrategy(databaseNamingStrategy, foreignKeyStrategy);
            BinLogParser binLogParser = new BinLogParser();
            BinLog binLog = new BinLog(
                    entityManager(),
                    microServiceName,
                    metadataStrategy,
                    binLogParser,
                    triggers
            );
            TransientResolverManager transientResolverManager =
                    new TransientResolverManager(
                            transientResolverProvider != null ?
                                    transientResolverProvider :
                                    DefaultTransientResolverProvider.INSTANCE
                    );
            JSqlClientImplementor sqlClient = new JSqlClientImpl(
                    connectionManager,
                    slaveConnectionManager,
                    dialect,
                    executor,
                    executorContextPrefixes,
                    sqlFormatter,
                    idGeneratorMap,
                    new ScalarProviderManager(typeScalarProviderMap, propScalarProviderMap, defaultEnumStrategy, dialect),
                    defaultBatchSize,
                    defaultListBatchSize,
                    offsetOptimizingThreshold,
                    null,
                    entityManager(),
                    caches,
                    triggers,
                    transactionTriggers,
                    metadataStrategy,
                    binLog,
                    filterManager,
                    transientResolverManager,
                    new DraftInterceptorManager(interceptors),
                    microServiceName,
                    microServiceExchange
            );
            filterManager.initialize(sqlClient);
            binLogParser.initialize(sqlClient, binLogObjectMapper);
            transientResolverManager.initialize(sqlClient);
            for (Initializer initializer : initializers) {
                try {
                    initializer.initialize(sqlClient);
                } catch (Exception ex) {
                    throw new ExecutionException(
                            "Failed to execute initializer after create sql client",
                            ex
                    );
                }
            }
            validateDatabase(metadataStrategy);
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
            for (ImmutableType type : entityManager().getAllTypes(microServiceName)) {
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

        private void validateAssociations(FilterManager filterManager) {
            for (ImmutableType type : entityManager().getAllTypes(microServiceName)) {
                if (type.isEntity()) {
                    for (ImmutableProp prop : type.getProps().values()) {
                        if (!prop.isNullable() && prop.isReference(TargetLevel.ENTITY) && !prop.isTransient()) {
                            if (prop.isRemote()) {
                                throw new ModelException(
                                        "Illegal reference association property \"" +
                                                prop +
                                                "\", it must be nullable because it is remote association"
                                );
                            }
                            if (filterManager.contains(prop.getTargetType())) {
                                throw new ModelException(
                                        "Illegal reference association property \"" +
                                                prop +
                                                "\", it must be nullable because the target type \"" +
                                                prop.getTargetType() +
                                                "\" may be handled by some global filters"
                                );
                            }
                        }
                    }
                }
            }
        }

        private void validateDatabase(MetadataStrategy metadataStrategy) {
            if (databaseValidationMode != DatabaseValidationMode.NONE) {
                ConnectionManager cm = connectionManager;
                if (cm == null) {
                    throw new IllegalStateException(
                            "The `connectionManager` of must be configured when `validate` is configured"
                    );
                }
                DatabaseValidationException validationException = cm.execute(con -> {
                    try {
                        return DatabaseValidators.validate(
                                entityManager(),
                                microServiceName,
                                metadataStrategy,
                                databaseValidationCatalog,
                                con
                        );
                    } catch (SQLException ex) {
                        throw new ExecutionException(
                                "Cannot validate the database because of SQL exception",
                                ex
                        );
                    }
                });
                if (validationException != null) {
                    if (databaseValidationMode == DatabaseValidationMode.ERROR) {
                        throw validationException;
                    }
                    LOGGER.warn(validationException.getMessage(), validationException);
                }
            }
        }

        private EntityManager entityManager() {
            EntityManager em = this.userEntityManager;
            if (em == null) {
                em = this.defaultEntityManager;
                if (em == null) {
                    em = EntityManager.fromResources(null, null);
                    this.defaultEntityManager = em;
                }
            }
            return em;
        }
    }
}

