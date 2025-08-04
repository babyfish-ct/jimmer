package org.babyfish.jimmer.sql;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.babyfish.jimmer.impl.util.ClassCache;
import org.babyfish.jimmer.impl.util.Classes;
import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.meta.*;
import org.babyfish.jimmer.sql.association.meta.AssociationProp;
import org.babyfish.jimmer.sql.association.meta.AssociationType;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.impl.EntitiesImpl;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableSymbol;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableSymbols;
import org.babyfish.jimmer.sql.ast.impl.mutation.AssociationsImpl;
import org.babyfish.jimmer.sql.ast.impl.mutation.MutableDeleteImpl;
import org.babyfish.jimmer.sql.ast.impl.mutation.MutableUpdateImpl;
import org.babyfish.jimmer.sql.ast.impl.query.*;
import org.babyfish.jimmer.sql.ast.impl.table.JWeakJoinLambdaFactory;
import org.babyfish.jimmer.sql.ast.impl.table.TableProxies;
import org.babyfish.jimmer.sql.ast.impl.table.WeakJoinHandle;
import org.babyfish.jimmer.sql.ast.impl.table.WeakJoinLambda;
import org.babyfish.jimmer.sql.ast.mutation.MutableDelete;
import org.babyfish.jimmer.sql.ast.mutation.MutableUpdate;
import org.babyfish.jimmer.sql.ast.query.MutableBaseQuery;
import org.babyfish.jimmer.sql.ast.query.MutableRecursiveBaseQuery;
import org.babyfish.jimmer.sql.ast.query.MutableRootQuery;
import org.babyfish.jimmer.sql.ast.query.MutableSubQuery;
import org.babyfish.jimmer.sql.ast.table.*;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.spi.TableLike;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;
import org.babyfish.jimmer.sql.cache.*;
import org.babyfish.jimmer.sql.di.*;
import org.babyfish.jimmer.sql.dialect.DefaultDialect;
import org.babyfish.jimmer.sql.dialect.Dialect;
import org.babyfish.jimmer.sql.event.TriggerType;
import org.babyfish.jimmer.sql.event.Triggers;
import org.babyfish.jimmer.sql.event.binlog.BinLog;
import org.babyfish.jimmer.sql.event.binlog.BinLogPropReader;
import org.babyfish.jimmer.sql.event.binlog.impl.BinLogImpl;
import org.babyfish.jimmer.sql.event.binlog.impl.BinLogParser;
import org.babyfish.jimmer.sql.event.impl.TriggersImpl;
import org.babyfish.jimmer.sql.exception.DatabaseValidationException;
import org.babyfish.jimmer.sql.exception.ExecutionException;
import org.babyfish.jimmer.sql.fetcher.ReferenceFetchType;
import org.babyfish.jimmer.sql.filter.Filter;
import org.babyfish.jimmer.sql.filter.FilterConfig;
import org.babyfish.jimmer.sql.filter.Filters;
import org.babyfish.jimmer.sql.filter.impl.FilterManager;
import org.babyfish.jimmer.sql.filter.impl.LogicalDeletedFilterProvider;
import org.babyfish.jimmer.sql.loader.graphql.Loaders;
import org.babyfish.jimmer.sql.loader.graphql.impl.LoadersImpl;
import org.babyfish.jimmer.sql.meta.*;
import org.babyfish.jimmer.sql.runtime.*;
import org.babyfish.jimmer.sql.transaction.Propagation;
import org.babyfish.jimmer.sql.transaction.TxConnectionManager;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.SQLException;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

class JSqlClientImpl implements JSqlClientImplementor {

    private final ConnectionManager connectionManager;

    private final ConnectionManager slaveConnectionManager;

    private final Dialect dialect;

    private final Executor executor;

    private final List<String> executorContextPrefixes;

    private final SqlFormatter sqlFormatter;

    private final ReferenceFetchType defaultReferenceFetchType;

    private final int maxJoinFetchDepth;

    private final ZoneId zoneId;

    private final Map<Class<?>, IdGenerator> idGeneratorMap;

    private final ScalarProviderManager scalarProviderManager;

    private final int defaultBatchSize;

    private final int defaultListBatchSize;

    private final boolean inListPaddingEnabled;

    private final boolean expandedInListPaddingEnabled;

    private final int offsetOptimizingThreshold;

    private final boolean reverseSortOptimizationEnabled;

    private final int maxCommandJoinCount;

    private final boolean mutationTransactionRequired;

    private final boolean targetTransferable;

    private final boolean explicitBatchEnabled;

    private final boolean dumbBatchAcceptable;

    private final boolean constraintViolationTranslatable;

    private final ExceptionTranslator<Exception> exceptionTranslator;

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

    private final DraftPreProcessorManager draftPreProcessorManager;

    private final DraftInterceptorManager draftInterceptorManager;

    private final String microServiceName;

    private final MicroServiceExchange microServiceExchange;

    private final Loaders loaders = new LoadersImpl(this);

    private final ReaderManager readerManager = new ReaderManager(this);

    private final ClassCache<Boolean> uniqueConstraintCache = new ClassCache<>(this::createUniqueConstraintUsed);

    private JSqlClientImpl(
            ConnectionManager connectionManager,
            ConnectionManager slaveConnectionManager,
            Dialect dialect,
            Executor executor,
            List<String> executorContextPrefixes,
            SqlFormatter sqlFormatter,
            ReferenceFetchType defaultReferenceFetchType,
            int maxJoinFetchDepth,
            ZoneId zoneId,
            Map<Class<?>, IdGenerator> idGeneratorMap,
            ScalarProviderManager scalarProviderManager,
            int defaultBatchSize,
            int defaultListBatchSize,
            boolean inListPaddingEnabled,
            boolean expandedInListPaddingEnabled,
            int offsetOptimizingThreshold,
            boolean reverseSortOptimizationEnabled,
            int maxCommandJoinCount,
            boolean mutationTransactionRequired,
            boolean targetTransferable,
            boolean explicitBatchEnabled,
            boolean dumbBatchAcceptable,
            boolean constraintViolationTranslatable,
            ExceptionTranslator<Exception> exceptionTranslator,
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
            DraftPreProcessorManager draftPreProcessorManager,
            DraftInterceptorManager draftInterceptorManager,
            String microServiceName,
            MicroServiceExchange microServiceExchange
    ) {
        this.connectionManager =
                connectionManager != null ?
                        connectionManager :
                        ConnectionManager.EXTERNAL_ONLY;
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
        this.defaultReferenceFetchType = defaultReferenceFetchType;
        this.maxJoinFetchDepth = maxJoinFetchDepth;
        this.zoneId = zoneId != null ? zoneId : ZoneId.systemDefault();
        this.idGeneratorMap = idGeneratorMap;
        this.scalarProviderManager = scalarProviderManager;
        this.defaultBatchSize = defaultBatchSize;
        this.defaultListBatchSize = defaultListBatchSize;
        this.inListPaddingEnabled = inListPaddingEnabled;
        this.expandedInListPaddingEnabled = expandedInListPaddingEnabled;
        this.offsetOptimizingThreshold = offsetOptimizingThreshold;
        this.reverseSortOptimizationEnabled = reverseSortOptimizationEnabled;
        this.maxCommandJoinCount = maxCommandJoinCount;
        this.mutationTransactionRequired = mutationTransactionRequired;
        this.targetTransferable = targetTransferable;
        this.explicitBatchEnabled = explicitBatchEnabled;
        this.dumbBatchAcceptable = dumbBatchAcceptable;
        this.constraintViolationTranslatable = constraintViolationTranslatable;
        this.exceptionTranslator = exceptionTranslator;
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
        this.draftPreProcessorManager = draftPreProcessorManager;
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
    public ReferenceFetchType getDefaultReferenceFetchType() {
        return defaultReferenceFetchType;
    }

    @Override
    public int getMaxJoinFetchDepth() {
        return maxJoinFetchDepth;
    }

    @Override
    public ZoneId getZoneId() {
        return zoneId;
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
    public boolean isInListPaddingEnabled() {
        return inListPaddingEnabled;
    }

    @Override
    public boolean isExpandedInListPaddingEnabled() {
        return expandedInListPaddingEnabled;
    }

    @Override
    public int getOffsetOptimizingThreshold() {
        return offsetOptimizingThreshold;
    }

    @Override
    public boolean isReverseSortOptimizationEnabled() {
        return reverseSortOptimizationEnabled;
    }

    @Override
    public int getMaxCommandJoinCount() {
        return maxCommandJoinCount;
    }

    @Override
    public boolean isTargetTransferable() {
        return targetTransferable;
    }

    @Override
    public boolean isBatchForbidden(boolean dumbBatchAcceptable) {
        if (!explicitBatchEnabled && dialect.isExplicitBatchRequired()) {
            return true;
        }
        Dialect dialect = this.dialect;
        if (!dialect.isBatchDumb()) {
            return false;
        }
        return !dumbBatchAcceptable && !this.dumbBatchAcceptable;
    }

    @Override
    public boolean isUpsertWithUniqueConstraintSupported(ImmutableType type) {
        return uniqueConstraintCache.get(type.getJavaClass());
    }

    @Override
    public boolean isConstraintViolationTranslatable() {
        return constraintViolationTranslatable;
    }

    @Override
    public boolean isMutationTransactionRequired() {
        return mutationTransactionRequired;
    }

    @Override
    @Nullable
    public ExceptionTranslator<Exception> getExceptionTranslator() {
        return exceptionTranslator;
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
    public <T extends BaseTable> MutableRootQuery<T> createQuery(T baseTable) {
        return new MutableRootQueryImpl<>(
                this,
                baseTable,
                ExecutionPurpose.QUERY,
                FilterLevel.DEFAULT
        );
    }

    @Override
    public MutableBaseQuery createBaseQuery(TableProxy<?> table) {
        return new MutableBaseQueryImpl(
                this,
                table
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends TableProxy<?>, R extends BaseTable> MutableRecursiveBaseQuery<R> createBaseQuery(
            T table,
            RecursiveRef<R> recursiveRef,
            WeakJoin<T, R> weakJoinLambda
    ) {
        WeakJoinLambda lambda = JWeakJoinLambdaFactory.get(weakJoinLambda);
        WeakJoinHandle handle = WeakJoinHandle.of(
                lambda,
                true,
                true,
                (WeakJoin<TableLike<?>, TableLike<?>>)(WeakJoin<?, ?>) weakJoinLambda
        );
        R recursiveTable = (R) BaseTableSymbols.of(recursiveRef, table, handle, JoinType.INNER);
        return new MutableRecursiveBaseQueryImpl<>(this, table, recursiveTable);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends TableProxy<?>, R extends BaseTable> MutableRecursiveBaseQuery<R> createBaseQuery(
            T table,
            RecursiveRef<R> recursiveRef,
            Class<? extends WeakJoin<T, R>> weakJoinType
    ) {
        WeakJoinHandle handle = WeakJoinHandle.of(weakJoinType);
        R recursiveTable = (R) BaseTableSymbols.of(recursiveRef, table, handle, JoinType.INNER);
        return new MutableRecursiveBaseQueryImpl<>(this, table, recursiveTable);
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
    public <R> R transaction(Propagation propagation, Supplier<R> block) {
        ConnectionManager connectionManager = this.connectionManager;
        if (!(connectionManager instanceof TxConnectionManager)) {
            throw new IllegalStateException(
                    "The current connection manager is not \"" +
                            TxConnectionManager.class.getName() +
                            "\""
            );
        }
        TxConnectionManager txConnectionManager = (TxConnectionManager) connectionManager;
        return txConnectionManager.executeTransaction(propagation, con -> block.get());
    }

    @Nullable
    @Override
    public DatabaseValidationException validateDatabase() {
        ConnectionManager cm = connectionManager;
        if (cm == null) {
            throw new IllegalStateException(
                    "The `connectionManager` of must be configured when `validate` is configured"
            );
        }
        return cm.execute(con -> {
            try {
                return DatabaseValidators.validate(
                        entityManager,
                        microServiceName,
                        defaultDissociationActionCheckable,
                        metadataStrategy,
                        null,
                        con
                );
            } catch (SQLException ex) {
                throw new ExecutionException(
                        "Cannot validate the database because of SQL exception",
                        ex
                );
            }
        });
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
                defaultReferenceFetchType,
                maxJoinFetchDepth,
                zoneId,
                idGeneratorMap,
                scalarProviderManager,
                defaultBatchSize,
                defaultListBatchSize,
                inListPaddingEnabled,
                expandedInListPaddingEnabled,
                offsetOptimizingThreshold,
                reverseSortOptimizationEnabled,
                maxCommandJoinCount,
                mutationTransactionRequired,
                targetTransferable,
                explicitBatchEnabled,
                dumbBatchAcceptable,
                constraintViolationTranslatable,
                exceptionTranslator,
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
                draftPreProcessorManager,
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
                defaultReferenceFetchType,
                maxJoinFetchDepth,
                zoneId,
                idGeneratorMap,
                scalarProviderManager,
                defaultBatchSize,
                defaultListBatchSize,
                inListPaddingEnabled,
                expandedInListPaddingEnabled,
                offsetOptimizingThreshold,
                reverseSortOptimizationEnabled,
                maxCommandJoinCount,
                mutationTransactionRequired,
                targetTransferable,
                explicitBatchEnabled,
                dumbBatchAcceptable,
                constraintViolationTranslatable,
                exceptionTranslator,
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
                draftPreProcessorManager,
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
                defaultReferenceFetchType,
                maxJoinFetchDepth,
                zoneId,
                idGeneratorMap,
                scalarProviderManager,
                defaultBatchSize,
                defaultListBatchSize,
                inListPaddingEnabled,
                expandedInListPaddingEnabled,
                offsetOptimizingThreshold,
                reverseSortOptimizationEnabled,
                maxCommandJoinCount,
                mutationTransactionRequired,
                targetTransferable,
                explicitBatchEnabled,
                dumbBatchAcceptable,
                constraintViolationTranslatable,
                exceptionTranslator,
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
                draftPreProcessorManager,
                draftInterceptorManager,
                microServiceName,
                microServiceExchange
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
                defaultReferenceFetchType,
                maxJoinFetchDepth,
                zoneId,
                idGeneratorMap,
                scalarProviderManager,
                defaultBatchSize,
                defaultListBatchSize,
                inListPaddingEnabled,
                expandedInListPaddingEnabled,
                offsetOptimizingThreshold,
                reverseSortOptimizationEnabled,
                maxCommandJoinCount,
                mutationTransactionRequired,
                targetTransferable,
                explicitBatchEnabled,
                dumbBatchAcceptable,
                constraintViolationTranslatable,
                exceptionTranslator,
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
                draftPreProcessorManager,
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
    public DraftPreProcessor<?> getDraftPreProcessor(ImmutableType type) {
        return draftPreProcessorManager.get(type);
    }

    @Nullable
    @Override
    public DraftInterceptor<?, ?> getDraftInterceptor(ImmutableType type) {
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

    private Boolean createUniqueConstraintUsed(Class<?> type) {
        KeyUniqueConstraint keyUniqueConstraint = type.getAnnotation(KeyUniqueConstraint.class);
        if (keyUniqueConstraint == null) {
            return false;
        }
        ImmutableType immutableType = ImmutableType.get(type);
        Map<String, Set<ImmutableProp>> keyGroupMap = immutableType.getKeyMatcher().toMap();
        if (keyGroupMap.isEmpty()) {
            throw new ModelException(
                    "Illegal type \"" +
                            immutableType +
                            "\", it cannot be decorated by \"@" +
                            KeyUniqueConstraint.class.getName() +
                            "\" because it have no property decorated by \"@" +
                            Key.class +
                            "\""
            );
        }
        if (keyUniqueConstraint.noMoreUniqueConstraints() && keyGroupMap.size() > 1) {
            throw new ModelException(
                    "Illegal type \"" +
                            immutableType +
                            "\", the argument `noMoreUniqueConstraints` of \"@" +
                            KeyUniqueConstraint.class.getName() +
                            "\" cannot be true because its properties belong to several key groups " +
                            keyGroupMap.keySet()
            );
        }
        return getDialect().isUpsertWithMultipleUniqueConstraintSupported() ||
            keyUniqueConstraint.noMoreUniqueConstraints();
    }

    public static class BuilderImpl implements JSqlClientImplementor.Builder {

        private static final Logger LOGGER = LoggerFactory.getLogger(BuilderImpl.class);

        private ConnectionManager connectionManager;

        private ConnectionManager slaveConnectionManager;

        private Dialect dialect = DefaultDialect.INSTANCE;

        private Executor executor;

        private List<String> executorContextPrefixes;

        private SqlFormatter sqlFormatter = SqlFormatter.SIMPLE;

        private ReferenceFetchType defaultReferenceFetchType = ReferenceFetchType.SELECT;

        private int maxJoinFetchDepth = 3;

        private ZoneId zoneId;

        private UserIdGeneratorProvider userIdGeneratorProvider;

        private LogicalDeletedValueGeneratorProvider logicalDeletedValueGeneratorProvider;

        private TransientResolverProvider transientResolverProvider;

        private final Map<Class<?>, ScalarProvider<?, ?>> typeScalarProviderMap = new HashMap<>();

        private final Map<ImmutableProp, ScalarProvider<?, ?>> propScalarProviderMap = new HashMap<>();

        private PropScalarProviderFactory propScalarProviderFactory;

        private final Map<Class<?>, ObjectMapper> serializedTypeObjectMapperMap = new HashMap<>();

        private final Map<ImmutableProp, ObjectMapper> serializedPropObjectMapperMap = new HashMap<>();

        private Function<ImmutableProp, ScalarProvider<?, ?>> defaultJsonProviderCreator;

        private final Map<Class<?>, IdGenerator> idGeneratorMap = new HashMap<>();

        private EnumType.Strategy defaultEnumStrategy = EnumType.Strategy.NAME;

        private DatabaseSchemaStrategy databaseSchemaStrategy = DatabaseSchemaStrategy.IMPLICIT;

        private DatabaseNamingStrategy databaseNamingStrategy = DefaultDatabaseNamingStrategy.UPPER_CASE;

        private MetaStringResolver metaStringResolver = MetaStringResolver.NO_OP;

        private int defaultBatchSize = DEFAULT_BATCH_SIZE;

        private int defaultListBatchSize = DEFAULT_LIST_BATCH_SIZE;

        private boolean inListPaddingEnabled;

        private boolean expandedInListPaddingEnabled;

        private int offsetOptimizingThreshold = Integer.MAX_VALUE;

        private boolean reverseSortOptimizationEnabled;

        private int maxCommandJoinCount = 2;

        private boolean mutationTransactionRequired;

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

        private final Collection<DraftPreProcessor<?>> processors = new ArrayList<>();

        private final List<DraftInterceptor<?, ?>> interceptors = new ArrayList<>();

        private ObjectMapper binLogObjectMapper;

        private final Map<ImmutableProp, BinLogPropReader> binLogPropReaderMap = new HashMap<>();

        private final Map<Class<?>, BinLogPropReader> typeBinLogPropReaderMap = new HashMap<>();

        private boolean isForeignKeyEnabledByDefault = true;

        private boolean targetTransferable;

        private boolean explicitBatchEnabled;

        private boolean dumbBatchAcceptable;

        private boolean constraintViolationTranslatable = true;

        private final Set<ExceptionTranslator<?>> exceptionTranslators = new LinkedHashSet<>();

        private final Set<Customizer> customizers = new LinkedHashSet<>();

        private final Set<Initializer> initializers = new LinkedHashSet<>();

        private DatabaseValidationMode databaseValidationMode = DatabaseValidationMode.NONE;

        private AopProxyProvider aopProxyProvider;

        private String microServiceName = "";

        private MicroServiceExchange microServiceExchange;

        public BuilderImpl() {}

        @Override
        public ConnectionManager getConnectionManager() {
            return connectionManager;
        }

        @Override
        public Dialect getDialect() {
            return dialect;
        }

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
        public JSqlClient.Builder setDefaultReferenceFetchType(ReferenceFetchType defaultReferenceFetchType) {
            if (defaultReferenceFetchType == null || defaultReferenceFetchType == ReferenceFetchType.AUTO) {
                throw new IllegalArgumentException("The default reference fetch type cannot be null or \"AUTO\"");
            }
            this.defaultReferenceFetchType = defaultReferenceFetchType;
            return this;
        }

        @Override
        @OldChain
        public Builder setMaxJoinFetchDepth(int maxJoinFetchDepth) {
            this.maxJoinFetchDepth = maxJoinFetchDepth;
            return this;
        }

        @Override
        public JSqlClient.Builder setZoneId(@Nullable ZoneId zoneId) {
            this.zoneId = zoneId;
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
                if (Classes.primitiveTypeOf((Class<?>) scalarType).isPrimitive() ||
                        scalarType == String.class ||
                        scalarType == BigInteger.class ||
                        scalarType == BigDecimal.class
                ) {
                    throw new IllegalStateException(
                            "Illegal global type scalar provider type \"" +
                                    scalarProvider.getClass().getName() +
                                    "\" its scalar type argument cannot be \"" +
                                    scalarType +
                                    "\" because it is " +
                                    (scalarType == String.class ? "string" : "numeric") +
                                    ". Please " +
                                    "use non-standard type or " +
                                    "use property level scalar provider"
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
        public JSqlClient.Builder addPropScalarProviderFactory(PropScalarProviderFactory factory) {
            this.propScalarProviderFactory = PropScalarProviderFactory
                    .combine(propScalarProviderFactory, factory);
            return this;
        }

        @Override
        public Builder setDefaultSerializedTypeObjectMapper(ObjectMapper mapper) {
            return setSerializedTypeObjectMapper(Object.class, mapper);
        }

        @Override
        public Builder setSerializedTypeObjectMapper(Class<?> type, ObjectMapper mapper) {
            if (mapper != null) {
                serializedTypeObjectMapperMap.put(type != null ? type : Object.class, mapper);
            } else {
                serializedTypeObjectMapperMap.remove(type != null ? type : Object.class);
            }
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
            if (mapper != null) {
                serializedPropObjectMapperMap.put(prop, mapper);
            } else {
                serializedTypeObjectMapperMap.remove(prop);
            }
            return this;
        }

        @Override
        public JSqlClient.Builder setDefaultJsonProviderCreator(Function<ImmutableProp, ScalarProvider<?, ?>> creator) {
            this.defaultJsonProviderCreator = creator;
            return this;
        }

        @Override
        public Builder setDefaultEnumStrategy(EnumType.Strategy strategy) {
            this.defaultEnumStrategy = strategy != null ? strategy : EnumType.Strategy.NAME;
            return this;
        }

        @Override
        public Builder setDatabaseSchemaStrategy(DatabaseSchemaStrategy strategy) {
            this.databaseSchemaStrategy = strategy != null ? strategy : DatabaseSchemaStrategy.IMPLICIT;
            return this;
        }

        @Override
        public Builder setDatabaseNamingStrategy(DatabaseNamingStrategy strategy) {
            this.databaseNamingStrategy = strategy != null ? strategy : DefaultDatabaseNamingStrategy.UPPER_CASE;
            return this;
        }

        @Override
        public JSqlClient.Builder setMetaStringResolver(MetaStringResolver resolver) {
            this.metaStringResolver = resolver != null ? resolver : MetaStringResolver.NO_OP;
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
        public JSqlClient.Builder setInListPaddingEnabled(boolean enabled) {
            this.inListPaddingEnabled = enabled;
            return this;
        }

        @Override
        public JSqlClient.Builder setExpandedInListPaddingEnabled(boolean enabled) {
            this.expandedInListPaddingEnabled = enabled;
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
        public JSqlClient.Builder setReverseSortOptimizationEnabled(boolean enabled) {
            reverseSortOptimizationEnabled = enabled;
            return this;
        }

        @Override
        public JSqlClient.Builder setMaxCommandJoinCount(int maxCommandJoinCount) {
            if (maxCommandJoinCount < 0 || maxCommandJoinCount > 8) {
                throw new IllegalArgumentException("maxCommandJoinCount must between 0 and 8");
            }
            this.maxCommandJoinCount = maxCommandJoinCount;
            return this;
        }

        @Override
        public JSqlClient.Builder setMutationTransactionRequired(boolean required) {
            this.mutationTransactionRequired = required;
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
        public JSqlClient.Builder addDraftPreProcessor(DraftPreProcessor<?> processor) {
            return addDraftPreProcessors(Collections.singleton(processor));
        }

        @Override
        public JSqlClient.Builder addDraftPreProcessors(DraftPreProcessor<?>... processors) {
            return addDraftPreProcessors(Arrays.asList(processors));
        }

        @Override
        public JSqlClient.Builder addDraftPreProcessors(Collection<DraftPreProcessor<?>> processors) {
            for (DraftPreProcessor<?> processor : processors) {
                if (processor != null) {
                    this.processors.add(processor);
                }
            }
            return this;
        }

        @Override
        public Builder addDraftInterceptor(DraftInterceptor<?, ?> interceptor) {
            return addDraftInterceptors(Collections.singletonList(interceptor));
        }

        @Override
        public Builder addDraftInterceptors(DraftInterceptor<?, ?>... interceptors) {
            return addDraftInterceptors(Arrays.asList(interceptors));
        }

        @Override
        public Builder addDraftInterceptors(Collection<? extends DraftInterceptor<?, ?>> interceptors) {
            for (DraftInterceptor<?, ?> interceptor : interceptors) {
                if (interceptor != null) {
                    this.interceptors.add(interceptor);
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
        public Builder setTargetTransferable(boolean targetTransferable) {
            this.targetTransferable = targetTransferable;
            return this;
        }

        @OldChain
        @Override
        public Builder setConstraintViolationTranslatable(boolean translatable) {
            constraintViolationTranslatable = translatable;
            return this;
        }

        @OldChain
        @Override
        public Builder addExceptionTranslator(ExceptionTranslator<?> translator) {
            if (translator != null) {
                this.exceptionTranslators.add(translator);
            }
            return this;
        }

        @Override
        public JSqlClient.Builder setExplicitBatchEnabled(boolean enabled) {
            explicitBatchEnabled = enabled;
            return this;
        }

        @Override
        public JSqlClient.Builder setDumbBatchAcceptable(boolean acceptable) {
            dumbBatchAcceptable = acceptable;
            return this;
        }

        @OldChain
        public Builder addExceptionTranslators(Collection<ExceptionTranslator<?>> translators) {
            for (ExceptionTranslator<?> translator : translators) {
                if (translator != null) {
                    this.exceptionTranslators.add(translator);
                }
            }
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
        public Builder setAopProxyProvider(AopProxyProvider provider) {
            this.aopProxyProvider = provider;
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
            ScalarProviderManager scalarProviderManager = new ScalarProviderManager(
                    typeScalarProviderMap,
                    propScalarProviderMap,
                    propScalarProviderFactory,
                    serializedTypeObjectMapperMap,
                    serializedPropObjectMapperMap,
                    defaultJsonProviderCreator,
                    defaultEnumStrategy,
                    dialect
            );
            MetadataStrategy metadataStrategy =
                    new MetadataStrategy(
                            databaseSchemaStrategy,
                            databaseNamingStrategy,
                            foreignKeyStrategy,
                            dialect,
                            scalarProviderManager,
                            metaStringResolver
                    );

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
            JSqlClientImplementor sqlClient = new JSqlClientImpl(
                    connectionManager,
                    slaveConnectionManager,
                    dialect,
                    executor,
                    executorContextPrefixes,
                    sqlFormatter,
                    defaultReferenceFetchType,
                    maxJoinFetchDepth,
                    zoneId,
                    idGeneratorMap,
                    scalarProviderManager,
                    defaultBatchSize,
                    defaultListBatchSize,
                    inListPaddingEnabled,
                    expandedInListPaddingEnabled,
                    offsetOptimizingThreshold,
                    reverseSortOptimizationEnabled,
                    maxCommandJoinCount,
                    mutationTransactionRequired,
                    targetTransferable,
                    explicitBatchEnabled,
                    dumbBatchAcceptable,
                    constraintViolationTranslatable,
                    ExceptionTranslator.of(exceptionTranslators),
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
                    new DraftPreProcessorManager(processors),
                    new DraftInterceptorManager(interceptors),
                    microServiceName,
                    microServiceExchange
            );
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
            validateDatabase(sqlClient);
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
                    new LogicalDeletedFilterProvider(
                            logicalDeletedBehavior,
                            Collections.emptyMap(),
                            Collections.emptyMap(),
                            microServiceName
                    ),
                    filters,
                    disabledFilters
            );
        }

        private void validateAssociations(FilterManager filterManager) {
            for (ImmutableType type : entityManager().getAllTypes(microServiceName)) {
                if (type.isEntity()) {
                    for (ImmutableProp prop : type.getProps().values()) {
                        if (!prop.isNullable()) {
                            Set<Filter<?>> filters = filterManager.getFiltersAffectNullity(prop);
                            if (!filters.isEmpty()) {
                                throw new ModelException(
                                        "Illegal reference association property \"" +
                                                prop +
                                                "\", it must be nullable because the target type \"" +
                                                prop.getTargetType() +
                                                "\" may be handled by filters: " +
                                                filters +
                                                ". If you only want let user cannot save null reference, " +
                                                "please specify the `inputNotNull` of " +
                                                (
                                                        prop.getAssociationAnnotation().annotationType() == OneToOne.class ?
                                                        OneToOne.class :
                                                        ManyToOne.class
                                                ).getName() +
                                                "; If you want to make a promise at the business level that " +
                                                "the association is not null to declare non-null reference, " +
                                                "you must ensure the above three points: " +
                                                "1. Let the filter implement a special empty interface: " +
                                                "AssociationIntegrityAssuranceFilter(java) or" +
                                                "KAssociationIntegrityAssuranceFilter(kotlin)" +
                                                "2. All filters on the associated object are association " +
                                                "integrity assurance filters" +
                                                "3. The current object applies all filters which have been" +
                                                "applied for associated object"
                                );
                            }
                        }
                    }
                }
            }
        }

        private void validateDatabase(JSqlClient sqlClient) {
            if (databaseValidationMode != DatabaseValidationMode.NONE) {
                DatabaseValidationException validationException = sqlClient.validateDatabase();
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
