package org.babyfish.jimmer.sql;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.meta.*;
import org.babyfish.jimmer.sql.association.meta.AssociationProp;
import org.babyfish.jimmer.sql.ast.impl.mutation.MutableDeleteImpl;
import org.babyfish.jimmer.sql.ast.impl.mutation.MutableUpdateImpl;
import org.babyfish.jimmer.sql.ast.impl.query.FilterLevel;
import org.babyfish.jimmer.sql.ast.impl.query.MutableRootQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.query.MutableSubQueryImpl;
import org.babyfish.jimmer.sql.ast.query.MutableSubQuery;
import org.babyfish.jimmer.sql.ast.table.TableEx;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;
import org.babyfish.jimmer.sql.cache.*;
import org.babyfish.jimmer.sql.di.*;
import org.babyfish.jimmer.sql.event.TriggerType;
import org.babyfish.jimmer.sql.event.Triggers;
import org.babyfish.jimmer.sql.event.impl.TriggersImpl;
import org.babyfish.jimmer.sql.event.binlog.BinLog;
import org.babyfish.jimmer.sql.event.binlog.impl.BinLogImpl;
import org.babyfish.jimmer.sql.event.binlog.impl.BinLogParser;
import org.babyfish.jimmer.sql.event.binlog.BinLogPropReader;
import org.babyfish.jimmer.sql.filter.Filter;
import org.babyfish.jimmer.sql.filter.FilterConfig;
import org.babyfish.jimmer.sql.filter.Filters;
import org.babyfish.jimmer.sql.filter.impl.FilterManager;
import org.babyfish.jimmer.sql.filter.impl.LogicalDeletedFilterProvider;
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
import org.babyfish.jimmer.sql.dialect.DefaultDialect;
import org.babyfish.jimmer.sql.dialect.Dialect;
import org.babyfish.jimmer.sql.meta.*;
import org.babyfish.jimmer.sql.runtime.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
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

    private final TriggersImpl triggers;

    private final TriggersImpl transactionTriggers;

    private final MetadataStrategy metadataStrategy;

    private final BinLog binLog;

    private final UserIdGeneratorProvider userIdGeneratorProvider;

    private final LogicalDeletedValueGeneratorProvider logicalDeletedValueGeneratorProvider;

    private final TransientResolverManager transientResolverManager;

    private final FilterManager filterManager;

    private final boolean defaultDissociationActionCheckable;

    private final IdOnlyTargetCheckingLevel idOnlyTargetCheckingLevel;

    private final boolean saveCommandPessimisticLock;

    private final DraftHandlerManager draftHandlerManager;

    private final String microServiceName;

    private final MicroServiceExchange microServiceExchange;

    private final Loaders loaders = new LoadersImpl(this);

    private final ReaderManager readerManager = new ReaderManager(this);

    private final ReadWriteLock initializationLock = new ReentrantReadWriteLock();

    private SqlClientInitializer sqlClientInitializer;

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
            TriggersImpl triggers,
            TriggersImpl transactionTriggers,
            MetadataStrategy metadataStrategy,
            BinLog binLog,
            FilterManager filterManager,
            UserIdGeneratorProvider userIdGeneratorProvider,
            LogicalDeletedValueGeneratorProvider logicalDeletedValueGeneratorProvider,
            TransientResolverManager transientResolverManager,
            boolean defaultDissociationActionCheckable,
            IdOnlyTargetCheckingLevel idOnlyTargetCheckingLevel,
            boolean saveCommandPessimisticLock,
            DraftHandlerManager draftHandlerManager,
            String microServiceName,
            MicroServiceExchange microServiceExchange,
            SqlClientInitializer sqlClientInitializer
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
        this.caches = caches;
        this.triggers = triggers;
        this.transactionTriggers = transactionTriggers;
        this.metadataStrategy = metadataStrategy;
        this.binLog = binLog;
        this.filterManager = filterManager;
        this.userIdGeneratorProvider =
                userIdGeneratorProvider != null ?
                        userIdGeneratorProvider :
                        new DefaultUserIdGeneratorProvider();
        this.logicalDeletedValueGeneratorProvider =
                logicalDeletedValueGeneratorProvider != null ?
                        logicalDeletedValueGeneratorProvider :
                        new DefaultLogicalDeletedValueGeneratorProvider();
        this.transientResolverManager = transientResolverManager;
        this.defaultDissociationActionCheckable = defaultDissociationActionCheckable;
        this.idOnlyTargetCheckingLevel = idOnlyTargetCheckingLevel;
        this.saveCommandPessimisticLock = saveCommandPessimisticLock;
        this.draftHandlerManager = draftHandlerManager;
        this.microServiceName = microServiceName;
        this.microServiceExchange = microServiceExchange;
        this.sqlClientInitializer = sqlClientInitializer;
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
                userIdGenerator = ImmutableType.get(entityType).getIdGenerator(this);
            }
        }
        return userIdGenerator;
    }

    @Override
    public <T extends SqlContext> T unwrap() {
        return null;
    }

    @Override
    public UserIdGenerator<?> getUserIdGenerator(String ref) throws Exception {
        return userIdGeneratorProvider.get(ref, this);
    }

    @SuppressWarnings("unchecked")
    @Override
    public UserIdGenerator<?> getUserIdGenerator(Class<?> userIdGeneratorType) throws Exception {
        return userIdGeneratorProvider.get((Class<UserIdGenerator<?>>) userIdGeneratorType, this);
    }

    @Override
    public LogicalDeletedValueGenerator<?> getLogicalDeletedValueGenerator(String ref) throws Exception {
        return logicalDeletedValueGeneratorProvider.get(ref, this);
    }

    @SuppressWarnings("unchecked")
    @Override
    public LogicalDeletedValueGenerator<?> getLogicalDeletedValueGenerator(Class<?> logicalDeletedValueGeneratorType) throws Exception {
        return logicalDeletedValueGeneratorProvider.get((Class<LogicalDeletedValueGenerator<?>>) logicalDeletedValueGeneratorType, this);
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
                FilterLevel.DEFAULT
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
                FilterLevel.DEFAULT
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
    public CacheOperator getCacheOperator() {
        return ((CachesImpl)caches).getOperator();
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
                userIdGeneratorProvider,
                logicalDeletedValueGeneratorProvider,
                transientResolverManager,
                defaultDissociationActionCheckable,
                idOnlyTargetCheckingLevel,
                saveCommandPessimisticLock,
                draftHandlerManager,
                microServiceName,
                microServiceExchange,
                sqlClientInitializer
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
                userIdGeneratorProvider,
                logicalDeletedValueGeneratorProvider,
                transientResolverManager,
                defaultDissociationActionCheckable,
                idOnlyTargetCheckingLevel,
                saveCommandPessimisticLock,
                draftHandlerManager,
                microServiceName,
                microServiceExchange,
                sqlClientInitializer
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
                userIdGeneratorProvider,
                logicalDeletedValueGeneratorProvider,
                transientResolverManager,
                defaultDissociationActionCheckable,
                idOnlyTargetCheckingLevel,
                saveCommandPessimisticLock,
                draftHandlerManager,
                microServiceName,
                microServiceExchange,
                sqlClientInitializer
        );
    }

    @Override
    public JSqlClientImplementor executor(Executor executor) {
        if (executor == null) {
            executor = DefaultExecutor.INSTANCE;
        }
        if (this.executor.equals(executor)) {
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
                filterManager,
                userIdGeneratorProvider,
                logicalDeletedValueGeneratorProvider,
                transientResolverManager,
                defaultDissociationActionCheckable,
                idOnlyTargetCheckingLevel,
                saveCommandPessimisticLock,
                draftHandlerManager,
                microServiceName,
                microServiceExchange,
                sqlClientInitializer
        );
    }

    @Override
    public TransientResolver<?, ?> getResolver(ImmutableProp prop) {
        return transientResolverManager.get(prop);
    }

    @Override
    public UserIdGeneratorProvider getUserIdGeneratorProvider() {
        return userIdGeneratorProvider;
    }

    @Override
    public StrategyProvider<TransientResolver<?, ?>> getTransientResolverProvider() {
        return transientResolverManager.getTransientResolverProvider();
    }

    @Override
    public Filters getFilters() {
        return filterManager;
    }

    @Override
    public boolean isDefaultDissociationActionCheckable() {
        return defaultDissociationActionCheckable;
    }

    @Override
    public IdOnlyTargetCheckingLevel getIdOnlyTargetCheckingLevel() {
        return idOnlyTargetCheckingLevel;
    }

    @Override
    public DraftHandler<?, ?> getDraftHandlers(ImmutableType type) {
        return draftHandlerManager.get(type);
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

    @Override
    public void initialize() {
        if (sqlClientInitializer != null) {
            sqlClientInitializer.initialize();
        }
    }

    public static class BuilderImpl implements JSqlClient.Builder {

        private static final Logger LOGGER = LoggerFactory.getLogger(BuilderImpl.class);

        private ConnectionManager connectionManager;

        private ConnectionManager slaveConnectionManager;

        private Dialect dialect = DefaultDialect.INSTANCE;

        private Executor executor;

        private List<String> executorContextPrefixes;

        private SqlFormatter sqlFormatter = SqlFormatter.SIMPLE;

        private UserIdGeneratorProvider userIdGeneratorProvider;

        private LogicalDeletedValueGeneratorProvider logicalDeletedValueGeneratorProvider;

        private TransientResolverProvider transientResolverProvider;

        private final Map<Class<?>, ScalarProvider<?, ?>> typeScalarProviderMap = new HashMap<>();

        private final Map<ImmutableProp, ScalarProvider<?, ?>> propScalarProviderMap = new HashMap<>();

        private final Map<Class<?>, ObjectMapper> serializedTypeObjectMapperMap = new HashMap<>();

        private final Map<ImmutableProp, ObjectMapper> serializedPropObjectMapperMap = new HashMap<>();

        private final Map<Class<?>, IdGenerator> idGeneratorMap = new HashMap<>();

        private EnumType.Strategy defaultEnumStrategy = EnumType.Strategy.NAME;

        private DatabaseNamingStrategy databaseNamingStrategy = DefaultDatabaseNamingStrategy.UPPER_CASE;

        private int defaultBatchSize = DEFAULT_BATCH_SIZE;

        private int defaultListBatchSize = DEFAULT_LIST_BATCH_SIZE;

        private int offsetOptimizingThreshold = Integer.MAX_VALUE;

        private EntityManager userEntityManager;

        private EntityManager defaultEntityManager;

        private final CacheConfig cacheConfig = new CacheConfig();

        private TriggerType triggerType = TriggerType.BINLOG_ONLY;

        private TriggersImpl triggers;

        private TriggersImpl transactionTriggers;

        private LogicalDeletedBehavior logicalDeletedBehavior = LogicalDeletedBehavior.DEFAULT;

        private final List<Filter<?>> filters = new ArrayList<>();

        private final Set<Filter<?>> disabledFilters = new HashSet<>();

        private boolean defaultDissociationActionCheckable = true;

        private IdOnlyTargetCheckingLevel idOnlyTargetCheckingLevel =
                IdOnlyTargetCheckingLevel.NONE;

        private boolean saveCommandPessimisticLock = false;

        private final List<DraftHandler<?, ?>> handlers = new ArrayList<>();

        private ObjectMapper binLogObjectMapper;

        private final Map<ImmutableProp, BinLogPropReader> binLogPropReaderMap = new HashMap<>();

        private final Map<Class<?>, BinLogPropReader> typeBinLogPropReaderMap = new HashMap<>();

        private boolean isForeignKeyEnabledByDefault = true;

        private final Set<Customizer> customizers = new LinkedHashSet<>();

        private final Set<Initializer> initializers = new LinkedHashSet<>();

        private DatabaseValidationMode databaseValidationMode = DatabaseValidationMode.NONE;

        private String databaseValidationCatalog;

        private String databaseValidationSchema;

        private AopProxyProvider aopProxyProvider;

        private String microServiceName = "";

        private MicroServiceExchange microServiceExchange;

        private InitializationType initializationType = InitializationType.IMMEDIATE;

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
        @OldChain
        public Builder setSqlFormatter(SqlFormatter sqlFormatter) {
            this.sqlFormatter = sqlFormatter != null ? sqlFormatter : SqlFormatter.SIMPLE;
            return this;
        }

        @Override
        @OldChain
        public Builder setUserIdGeneratorProvider(UserIdGeneratorProvider userIdGeneratorProvider) {
            this.userIdGeneratorProvider = userIdGeneratorProvider;
            return this;
        }

        @Override
        @OldChain
        public Builder setLogicalDeletedValueGeneratorProvider(LogicalDeletedValueGeneratorProvider logicalDeletedValueGeneratorProvider) {
            this.logicalDeletedValueGeneratorProvider = logicalDeletedValueGeneratorProvider;
            return this;
        }

        @Override
        @OldChain
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
        public Builder setScalarProvider(TypedProp<?, ?> prop, ScalarProvider<?, ?> scalarProvider) {
            if (prop == null) {
                throw new IllegalArgumentException("prop cannot be null");
            }
            addScalarProviderImpl(prop.unwrap(), scalarProvider);
            return this;
        }

        @Override
        public Builder setScalarProvider(ImmutableProp prop, ScalarProvider<?, ?> scalarProvider) {
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
        public Builder setDefaultSerializedTypeObjectMapper(ObjectMapper mapper) {
            return setSerializedTypeObjectMapper(Object.class, mapper);
        }

        @Override
        public Builder setSerializedTypeObjectMapper(Class<?> type, ObjectMapper mapper) {
            serializedTypeObjectMapperMap.put(type != null ? type : Object.class, mapper);
            return this;
        }

        @Override
        public Builder setSerializedPropObjectMapper(TypedProp<?, ?> prop, ObjectMapper mapper) {
            return setSerializedPropObjectMapper(prop.unwrap(), mapper);
        }

        @Override
        public Builder setSerializedPropObjectMapper(ImmutableProp prop, ObjectMapper mapper) {
            if (prop.getAnnotation(Serialized.class) == null) {
                throw new IllegalArgumentException(
                        "Cannot set the serialized property object mapper for \"" +
                                prop +
                                "\" because it is not decorated by \"@" +
                                Serialized.class.getName() +
                                "\""
                );
            }
            serializedPropObjectMapperMap.put(prop, mapper);
            return this;
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
            if (threshold < 0) {
                throw new IllegalArgumentException("`threshold` cannot be negative number");
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
            this.userEntityManager = entityManager;
            return this;
        }

        @Override
        @OldChain
        public JSqlClient.Builder setCaches(Consumer<CacheConfig> block) {
            block.accept(cacheConfig);
            return this;
        }

        @Override
        public Builder setCacheFactory(CacheFactory cacheFactory) {
            cacheConfig.setCacheFactory(cacheFactory);
            return this;
        }

        @Override
        public Builder setCacheOperator(CacheOperator cacheOperator) {
            cacheConfig.setCacheOperator(cacheOperator);
            return this;
        }

        @Override
        public Builder addCacheAbandonedCallback(CacheAbandonedCallback callback) {
            cacheConfig.addAbandonedCallback(callback);
            return this;
        }

        @Override
        public Builder addCacheAbandonedCallbacks(Collection<? extends CacheAbandonedCallback> callbacks) {
            cacheConfig.addAbandonedCallbacks(callbacks);
            return this;
        }

        @Override
        public Builder setTriggerType(TriggerType triggerType) {
            this.triggerType = triggerType != null ? triggerType : TriggerType.BINLOG_ONLY;
            return this;
        }

        @Override
        public Builder setLogicalDeletedBehavior(LogicalDeletedBehavior behavior) {
            this.logicalDeletedBehavior = behavior != null ? behavior : LogicalDeletedBehavior.DEFAULT;
            return this;
        }

        @Override
        public Builder addFilters(Filter<?>... filters) {
            return addFilters(Arrays.asList(filters));
        }

        @Override
        public Builder addFilters(Collection<? extends Filter<?>> filters) {
            for (Filter<?> filter : filters) {
                if (filter != null) {
                    if (filter instanceof FilterManager.Exported) {
                        throw new IllegalArgumentException("Cannot add filter which is exported by filter manager");
                    }
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
        public Builder addDisabledFilters(Collection<? extends Filter<?>> filters) {
            for (Filter<?> filter : filters) {
                if (filter != null) {
                    this.filters.add(filter);
                    this.disabledFilters.add(filter);
                }
            }
            return this;
        }

        @Override
        public Builder setDefaultDissociateActionCheckable(boolean checkable) {
            defaultDissociationActionCheckable = checkable;
            return this;
        }

        @Override
        public Builder setIdOnlyTargetCheckingLevel(IdOnlyTargetCheckingLevel checkingLevel) {
            idOnlyTargetCheckingLevel = checkingLevel != null ?
                    checkingLevel :
                    IdOnlyTargetCheckingLevel.NONE;
            return this;
        }

        @Override
        public Builder setSaveCommandPessimisticLock() {
            return setSaveCommandPessimisticLock(true);
        }

        @Override
        public Builder setSaveCommandPessimisticLock(boolean lock) {
            saveCommandPessimisticLock = lock;
            return this;
        }

        @Override
        public Builder addDraftHandler(DraftHandler<?, ?> handler) {
            return addDraftHandlers(Collections.singletonList(handler));
        }

        @Override
        public Builder addDraftHandlers(DraftHandler<?, ?>... handlers) {
            return addDraftHandlers(Arrays.asList(handlers));
        }

        @Override
        public Builder addDraftHandlers(Collection<? extends DraftHandler<?, ?>> handlers) {
            for (DraftHandler<?, ?> handler : handlers) {
                if (handler != null) {
                    this.handlers.add(handler);
                }
            }
            return this;
        }

        @Override
        public Builder setDefaultBinLogObjectMapper(ObjectMapper mapper) {
            this.binLogObjectMapper = mapper;
            return this;
        }

        @Override
        public Builder setBinLogPropReader(ImmutableProp prop, BinLogPropReader reader) {
            if (prop.isEmbedded(EmbeddedLevel.BOTH)) {
                throw new IllegalArgumentException(
                        "Cannot set bin log reader for embedded property \"" +
                                prop +
                                "\""
                );
            }
            if (!prop.isScalar(TargetLevel.ENTITY)) {
                throw new IllegalArgumentException(
                        "Cannot set bin log reader for non-scalar property \"" +
                                prop +
                                "\""
                );
            }
            if (!prop.isColumnDefinition()) {
                throw new IllegalArgumentException(
                        "Cannot set bin log reader for property \"" +
                                prop +
                                "\" which is not column definition"
                );
            }
            if (prop instanceof AssociationProp) {
                throw new IllegalArgumentException(
                        "Cannot set bin log reader for association property \"" +
                                prop +
                                "\""
                );
            }
            binLogPropReaderMap.put(prop, reader);
            return this;
        }

        @Override
        public Builder setBinLogPropReader(TypedProp.Scalar<?, ?> prop, BinLogPropReader reader) {
            return setBinLogPropReader(prop.unwrap(), reader);
        }

        @Override
        public Builder setBinLogPropReader(Class<?> propType, BinLogPropReader reader) {
            if (propType == void.class) {
                throw new IllegalArgumentException(
                        "Cannot set bin log reader for void type"
                );
            }
            typeBinLogPropReaderMap.put(propType, reader);
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
        public Builder addCustomizers(Collection<? extends Customizer> customizers) {
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
        public Builder addInitializers(Collection<? extends Initializer> initializers) {
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
        public Builder setDatabaseValidationSchema(String schema) {
            this.databaseValidationSchema = schema != null && !schema.isEmpty() ? schema : null;
            return this;
        }

        @Override
        public Builder setAopProxyProvider(AopProxyProvider provider) {
            this.aopProxyProvider = aopProxyProvider;
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
        public Builder setInitializationType(InitializationType type) {
            this.initializationType = type != null ? type : InitializationType.IMMEDIATE;
            return this;
        }

        @Override
        public JSqlClient build() {
            if (!microServiceName.isEmpty() && microServiceExchange == null) {
                throw new IllegalStateException(
                        "The `microServiceExchange` must be configured when `microServiceName` is configured"
                );
            }
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

            entityManager().validate(metadataStrategy);

            FilterManager filterManager = createFilterManager();
            validateAssociations(filterManager);

            createTriggers();
            Caches caches = CachesImpl.of(
                    cacheConfig,
                    microServiceName,
                    entityManager(),
                    triggers,
                    filterManager
            );
            BinLogParser binLogParser = new BinLogParser();
            BinLog binLog = new BinLogImpl(
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
                                    DefaultTransientResolverProvider.INSTANCE,
                            aopProxyProvider
                    );
            SqlClientInitializer sqlClientInitializer = null;
            if (initializationType == InitializationType.MANUAL) {
                sqlClientInitializer = new SqlClientInitializer();
            }
            JSqlClientImplementor sqlClient = new JSqlClientImpl(
                    connectionManager,
                    slaveConnectionManager,
                    dialect,
                    executor,
                    executorContextPrefixes,
                    sqlFormatter,
                    idGeneratorMap,
                    new ScalarProviderManager(
                            typeScalarProviderMap,
                            propScalarProviderMap,
                            serializedTypeObjectMapperMap,
                            serializedPropObjectMapperMap,
                            defaultEnumStrategy,
                            dialect
                    ),
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
                    userIdGeneratorProvider,
                    logicalDeletedValueGeneratorProvider,
                    transientResolverManager,
                    defaultDissociationActionCheckable,
                    idOnlyTargetCheckingLevel,
                    saveCommandPessimisticLock,
                    new DraftHandlerManager(handlers),
                    microServiceName,
                    microServiceExchange,
                    sqlClientInitializer
            );
            Runnable initializationAction = () -> {
                CachesImpl.initialize(caches, sqlClient);
                filterManager.initialize(sqlClient);
                binLogParser.initialize(sqlClient, binLogObjectMapper, binLogPropReaderMap, typeBinLogPropReaderMap);
                transientResolverManager.initialize(sqlClient);
                triggers.initialize(sqlClient);
                if (transactionTriggers != null && transactionTriggers != triggers) {
                    transactionTriggers.initialize(sqlClient);
                }
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
            };
            if (sqlClientInitializer != null) {
                sqlClientInitializer.setAction(initializationAction);
            } else {
                initializationAction.run();
            }
            return sqlClient;
        }

        private void createTriggers() {
            if (triggers == null) {
                switch (triggerType) {
                    case TRANSACTION_ONLY:
                        transactionTriggers = triggers = new TriggersImpl(true);
                        break;
                    case BOTH:
                        triggers = new TriggersImpl(false);
                        transactionTriggers = new TriggersImpl(true);
                        break;
                    default:
                        triggers = new TriggersImpl(false);
                        break;
                }
            }
        }

        private FilterManager createFilterManager() {
            return new FilterManager(
                    aopProxyProvider,
                    new LogicalDeletedFilterProvider(logicalDeletedBehavior, entityManager(), microServiceName),
                    filters,
                    disabledFilters
            );
        }

        private void validateAssociations(FilterManager filterManager) {
            for (ImmutableType type : entityManager().getAllTypes(microServiceName)) {
                if (type.isEntity()) {
                    for (ImmutableProp prop : type.getProps().values()) {
                        if (!prop.isNullable() && filterManager.isNullableRequired(prop)) {
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
                                defaultDissociationActionCheckable,
                                metadataStrategy,
                                databaseValidationCatalog,
                                databaseValidationSchema,
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

    private static class SqlClientInitializer {

        private final ReadWriteLock rwl = new ReentrantReadWriteLock();

        private Runnable action;

        public void setAction(Runnable action) {
            if (this.action != null) {
                throw new IllegalStateException("action has already been set");
            }
            if (action == null) {
                throw new IllegalArgumentException("action cannot be null");
            }
            this.action = action;
        }

        private void initialize() {
            Lock lock;

            (lock = rwl.readLock()).lock();
            try {
                if (action == null) {
                    return;
                }
            } finally {
                lock.unlock();
            }

            (lock = rwl.writeLock()).lock();
            try {
                if (action == null) {
                    return;
                }
                action.run();
                action = null;
            } finally {
                lock.unlock();
            }
        }
    }
}

