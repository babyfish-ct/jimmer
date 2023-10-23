package org.babyfish.jimmer.sql.di;

import org.babyfish.jimmer.Input;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.*;
import org.babyfish.jimmer.sql.association.meta.AssociationType;
import org.babyfish.jimmer.sql.ast.mutation.*;
import org.babyfish.jimmer.sql.ast.query.MutableRootQuery;
import org.babyfish.jimmer.sql.ast.query.MutableSubQuery;
import org.babyfish.jimmer.sql.ast.table.AssociationTable;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.TableEx;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;
import org.babyfish.jimmer.sql.cache.*;
import org.babyfish.jimmer.sql.dialect.Dialect;
import org.babyfish.jimmer.sql.event.TriggerType;
import org.babyfish.jimmer.sql.event.Triggers;
import org.babyfish.jimmer.sql.event.binlog.BinLog;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.filter.FilterConfig;
import org.babyfish.jimmer.sql.filter.Filters;
import org.babyfish.jimmer.sql.loader.graphql.Loaders;
import org.babyfish.jimmer.sql.meta.*;
import org.babyfish.jimmer.sql.runtime.*;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

public abstract class AbstractJSqlClientWrapper implements JSqlClientImplementor {

    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    private JSqlClientImplementor sqlClient;

    private JSqlClientImplementor sqlClient() {
        Lock lock;
        JSqlClientImplementor sqlClient;

        (lock = readWriteLock.readLock()).lock();
        try {
            sqlClient = this.sqlClient;
        } finally {
            lock.unlock();
        }

        if (sqlClient == null) {
            (lock = readWriteLock.writeLock()).lock();
            try {
                sqlClient = this.sqlClient;
                if (sqlClient == null) {
                    JSqlClient.Builder builder = createBuilder();
                    builder.setInitializationType(InitializationType.MANUAL);
                    sqlClient = (JSqlClientImplementor) builder.build();
                    afterCreate(sqlClient);
                    this.sqlClient = sqlClient;
                }
            } finally {
                lock.unlock();
            }
        }

        return sqlClient;
    }

    protected abstract JSqlClient.Builder createBuilder();

    protected void afterCreate(JSqlClientImplementor sqlClient) {}

    @SuppressWarnings("unchecked")
    @Override
    public <T extends SqlContext> T unwrap() {
        return (T) sqlClient();
    }

    @Override
    public UserIdGenerator<?> getUserIdGenerator(String ref) throws Exception {
        return sqlClient().getUserIdGenerator(ref);
    }

    @Override
    public UserIdGenerator<?> getUserIdGenerator(Class<?> userIdGenerator) throws Exception {
        return sqlClient().getUserIdGenerator(userIdGenerator);
    }

    public static Builder newBuilder() {
        return JSqlClient.newBuilder();
    }

    @Override
    public <T extends TableProxy<?>> MutableRootQuery<T> createQuery(T table) {
        return sqlClient().createQuery(table);
    }

    @Override
    public MutableUpdate createUpdate(TableProxy<?> table) {
        return sqlClient().createUpdate(table);
    }

    @Override
    public MutableDelete createDelete(TableProxy<?> table) {
        return sqlClient().createDelete(table);
    }

    @Override
    public <SE, ST extends Table<SE>, TE, TT extends Table<TE>> MutableRootQuery<AssociationTable<SE, ST, TE, TT>> createAssociationQuery(AssociationTable<SE, ST, TE, TT> table) {
        return sqlClient().createAssociationQuery(table);
    }

    @Override
    public Entities getEntities() {
        return sqlClient().getEntities();
    }

    @Override
    public Triggers getTriggers() {
        return sqlClient().getTriggers();
    }

    @Override
    public Triggers getTriggers(boolean transaction) {
        return sqlClient().getTriggers(transaction);
    }

    @Override
    public Associations getAssociations(TypedProp.Association<?, ?> prop) {
        return sqlClient().getAssociations(prop);
    }

    @Override
    public Associations getAssociations(ImmutableProp immutableProp) {
        return sqlClient().getAssociations(immutableProp);
    }

    @Override
    public Associations getAssociations(AssociationType associationType) {
        return sqlClient().getAssociations(associationType);
    }

    @Override
    public Caches getCaches() {
        return sqlClient().getCaches();
    }

    @Override
    public Filters getFilters() {
        return sqlClient().getFilters();
    }

    @Override
    public BinLog getBinLog() {
        return sqlClient().getBinLog();
    }

    @Override
    public <E> @Nullable E findById(Class<E> type, Object id) {
        return sqlClient().findById(type, id);
    }

    @Override
    public <E> @Nullable E findById(Fetcher<E> fetcher, Object id) {
        return sqlClient().findById(fetcher, id);
    }

    @Override
    public <E> List<E> findByIds(Class<E> type, Collection<?> ids) {
        return sqlClient().findByIds(type, ids);
    }

    @Override
    public <E> List<E> findByIds(Fetcher<E> fetcher, Collection<?> ids) {
        return sqlClient().findByIds(fetcher, ids);
    }

    @Override
    public @Nullable <K, V> Map<K, V> findMapByIds(Class<V> type, Collection<K> ids) {
        return sqlClient().findMapByIds(type, ids);
    }

    @Override
    public @Nullable <K, V> Map<K, V> findMapByIds(Fetcher<V> fetcher, Collection<K> ids) {
        return sqlClient().findMapByIds(fetcher, ids);
    }

    @Override
    public <E> SimpleSaveResult<E> save(E entity, SaveMode mode) {
        return sqlClient().save(entity, mode);
    }

    @Override
    public <E> SimpleSaveResult<E> save(E entity) {
        return sqlClient().save(entity);
    }

    @Override
    public <E> SimpleSaveResult<E> insert(E entity) {
        return sqlClient().insert(entity);
    }

    @Override
    public <E> SimpleSaveResult<E> update(E entity) {
        return sqlClient().update(entity);
    }

    @Override
    public <E> SimpleSaveResult<E> save(Input<E> input, SaveMode mode) {
        return sqlClient().save(input, mode);
    }

    @Override
    public <E> SimpleSaveResult<E> save(Input<E> input) {
        return sqlClient().save(input);
    }

    @Override
    public <E> SimpleSaveResult<E> insert(Input<E> input) {
        return sqlClient().insert(input);
    }

    @Override
    public <E> SimpleSaveResult<E> update(Input<E> input) {
        return sqlClient().update(input);
    }

    @Override
    public DeleteResult deleteById(Class<?> type, Object id, DeleteMode mode) {
        return sqlClient().deleteById(type, id, mode);
    }

    @Override
    public DeleteResult deleteById(Class<?> type, Object id) {
        return sqlClient().deleteById(type, id);
    }

    @Override
    public DeleteResult deleteByIds(Class<?> type, Collection<?> ids, DeleteMode mode) {
        return sqlClient().deleteByIds(type, ids, mode);
    }

    @Override
    public DeleteResult deleteByIds(Class<?> type, Collection<?> ids) {
        return sqlClient().deleteByIds(type, ids);
    }

    @Override
    public MutableSubQuery createSubQuery(TableProxy<?> table) {
        return sqlClient().createSubQuery(table);
    }

    @Override
    public <SE, ST extends TableEx<SE>, TE, TT extends TableEx<TE>> MutableSubQuery createAssociationSubQuery(AssociationTable<SE, ST, TE, TT> table) {
        return sqlClient().createAssociationSubQuery(table);
    }

    @Override
    public ConnectionManager getConnectionManager() {
        return sqlClient().getConnectionManager();
    }

    @Override
    public ConnectionManager getSlaveConnectionManager(boolean forUpdate) {
        return sqlClient().getSlaveConnectionManager(forUpdate);
    }

    @Override
    public Dialect getDialect() {
        return sqlClient().getDialect();
    }

    @Override
    public Executor getExecutor() {
        return sqlClient().getExecutor();
    }

    @Override
    public EntityManager getEntityManager() {
        return sqlClient().getEntityManager();
    }

    @Override
    public MetadataStrategy getMetadataStrategy() {
        return sqlClient().getMetadataStrategy();
    }

    @Override
    public List<String> getExecutorContextPrefixes() {
        return sqlClient().getExecutorContextPrefixes();
    }

    @Override
    public SqlFormatter getSqlFormatter() {
        return sqlClient().getSqlFormatter();
    }

    @Override
    public CacheOperator getCacheOperator() {
        return sqlClient().getCacheOperator();
    }

    @Override
    public TriggerType getTriggerType() {
        return sqlClient().getTriggerType();
    }

    @Override
    public <T, S> ScalarProvider<T, S> getScalarProvider(Class<T> scalarType) {
        return sqlClient().getScalarProvider(scalarType);
    }

    @Override
    public <T, S> ScalarProvider<T, S> getScalarProvider(TypedProp<T, ?> prop) {
        return sqlClient().getScalarProvider(prop);
    }

    @Override
    public <T, S> ScalarProvider<T, S> getScalarProvider(ImmutableProp prop) {
        return sqlClient().getScalarProvider(prop);
    }

    @Override
    public IdGenerator getIdGenerator(Class<?> entityType) {
        return sqlClient().getIdGenerator(entityType);
    }

    @Override
    public int getDefaultBatchSize() {
        return sqlClient().getDefaultBatchSize();
    }

    @Override
    public int getDefaultListBatchSize() {
        return sqlClient().getDefaultListBatchSize();
    }

    @Override
    public int getOffsetOptimizingThreshold() {
        return sqlClient().getOffsetOptimizingThreshold();
    }

    @Override
    public TransientResolver<?, ?> getResolver(ImmutableProp prop) {
        return sqlClient().getResolver(prop);
    }

    @Override
    public StrategyProvider<UserIdGenerator<?>> getUserIdGeneratorProvider() {
        return sqlClient().getUserIdGeneratorProvider();
    }

    @Override
    public StrategyProvider<TransientResolver<?, ?>> getTransientResolverProvider() {
        return sqlClient().getTransientResolverProvider();
    }

    @Override
    public IdOnlyTargetCheckingLevel getIdOnlyTargetCheckingLevel() {
        return sqlClient().getIdOnlyTargetCheckingLevel();
    }

    @Override
    public DraftHandler<?, ?> getDraftHandlers(ImmutableType type) {
        return sqlClient().getDraftHandlers(type);
    }

    @Override
    public Reader<?> getReader(Class<?> type) {
        return sqlClient().getReader(type);
    }

    @Override
    public Reader<?> getReader(ImmutableType type) {
        return sqlClient().getReader(type);
    }

    @Override
    public Reader<?> getReader(ImmutableProp prop) {
        return sqlClient().getReader(prop);
    }

    @Override
    public String getMicroServiceName() {
        return sqlClient().getMicroServiceName();
    }

    @Override
    public MicroServiceExchange getMicroServiceExchange() {
        return sqlClient().getMicroServiceExchange();
    }

    @Override
    public JSqlClientImplementor caches(Consumer<CacheDisableConfig> block) {
        return sqlClient().caches(block);
    }

    @Override
    public JSqlClientImplementor filters(Consumer<FilterConfig> block) {
        return sqlClient().filters(block);
    }

    @Override
    public JSqlClientImplementor disableSlaveConnectionManager() {
        return sqlClient().disableSlaveConnectionManager();
    }

    @Override
    public Loaders getLoaders() {
        return sqlClient().getLoaders();
    }

    @Override
    public void initialize() {
        sqlClient().initialize();
    }
}
