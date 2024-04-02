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
import org.babyfish.jimmer.sql.cache.CacheDisableConfig;
import org.babyfish.jimmer.sql.cache.CacheOperator;
import org.babyfish.jimmer.sql.cache.Caches;
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

import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public abstract class AbstractJSqlClientDelegate implements JSqlClientImplementor {

    protected abstract JSqlClientImplementor sqlClient();

    public <T extends SqlContext> T unwrap() {
        return sqlClient().unwrap();
    }

    public UserIdGenerator<?> getUserIdGenerator(String ref) throws Exception {
        return sqlClient().getUserIdGenerator(ref);
    }

    public UserIdGenerator<?> getUserIdGenerator(Class<?> userIdGeneratorType) throws Exception {
        return sqlClient().getUserIdGenerator(userIdGeneratorType);
    }

    public LogicalDeletedValueGenerator<?> getLogicalDeletedValueGenerator(String ref) throws Exception {
        return sqlClient().getLogicalDeletedValueGenerator(ref);
    }

    public LogicalDeletedValueGenerator<?> getLogicalDeletedValueGenerator(Class<?> logicalDeletedValueGeneratorType) throws Exception {
        return sqlClient().getLogicalDeletedValueGenerator(logicalDeletedValueGeneratorType);
    }

    public <T extends TableProxy<?>> MutableRootQuery<T> createQuery(T table) {
        return sqlClient().createQuery(table);
    }

    public MutableUpdate createUpdate(TableProxy<?> table) {
        return sqlClient().createUpdate(table);
    }

    public MutableDelete createDelete(TableProxy<?> table) {
        return sqlClient().createDelete(table);
    }

    public <SE, ST extends Table<SE>, TE, TT extends Table<TE>> MutableRootQuery<AssociationTable<SE, ST, TE, TT>> createAssociationQuery(AssociationTable<SE, ST, TE, TT> table) {
        return sqlClient().createAssociationQuery(table);
    }

    public Entities getEntities() {
        return sqlClient().getEntities();
    }

    public Triggers getTriggers() {
        return sqlClient().getTriggers();
    }

    public Triggers getTriggers(boolean transaction) {
        return sqlClient().getTriggers(transaction);
    }

    public Associations getAssociations(TypedProp.Association<?, ?> prop) {
        return sqlClient().getAssociations(prop);
    }

    public Associations getAssociations(ImmutableProp immutableProp) {
        return sqlClient().getAssociations(immutableProp);
    }

    public Associations getAssociations(AssociationType associationType) {
        return sqlClient().getAssociations(associationType);
    }

    public Caches getCaches() {
        return sqlClient().getCaches();
    }

    public Filters getFilters() {
        return sqlClient().getFilters();
    }

    public BinLog getBinLog() {
        return sqlClient().getBinLog();
    }

    public <E> @Nullable E findById(Class<E> type, Object id) {
        return sqlClient().findById(type, id);
    }

    public <E> @Nullable E findById(Fetcher<E> fetcher, Object id) {
        return sqlClient().findById(fetcher, id);
    }

    public <E> List<E> findByIds(Class<E> type, Collection<?> ids) {
        return sqlClient().findByIds(type, ids);
    }

    public <E> List<E> findByIds(Fetcher<E> fetcher, Collection<?> ids) {
        return sqlClient().findByIds(fetcher, ids);
    }

    public @Nullable <K, V> Map<K, V> findMapByIds(Class<V> type, Collection<K> ids) {
        return sqlClient().findMapByIds(type, ids);
    }

    public @Nullable <K, V> Map<K, V> findMapByIds(Fetcher<V> fetcher, Collection<K> ids) {
        return sqlClient().findMapByIds(fetcher, ids);
    }

    public <E> SimpleSaveResult<E> save(E entity, SaveMode mode) {
        return sqlClient().save(entity, mode);
    }

    public <E> SimpleSaveResult<E> save(E entity) {
        return sqlClient().save(entity);
    }

    public <E> SimpleSaveResult<E> insert(E entity) {
        return sqlClient().insert(entity);
    }

    public <E> SimpleSaveResult<E> update(E entity) {
        return sqlClient().update(entity);
    }

    public <E> SimpleSaveResult<E> save(Input<E> input, SaveMode mode) {
        return sqlClient().save(input, mode);
    }

    public <E> SimpleSaveResult<E> save(Input<E> input) {
        return sqlClient().save(input);
    }

    public <E> SimpleSaveResult<E> insert(Input<E> input) {
        return sqlClient().insert(input);
    }

    public <E> SimpleSaveResult<E> update(Input<E> input) {
        return sqlClient().update(input);
    }

    public <E> SimpleSaveResult<E> merge(E entity) {
        return sqlClient().merge(entity);
    }

    public <E> SimpleSaveResult<E> merge(Input<E> input) {
        return sqlClient().merge(input);
    }

    public <E> SimpleSaveResult<E> merge(E entity, SaveMode mode) {
        return sqlClient().merge(entity, mode);
    }

    public <E> SimpleSaveResult<E> merge(Input<E> input, SaveMode mode) {
        return sqlClient().merge(input, mode);
    }

    public DeleteResult deleteById(Class<?> type, Object id, DeleteMode mode) {
        return sqlClient().deleteById(type, id, mode);
    }

    public DeleteResult deleteById(Class<?> type, Object id) {
        return sqlClient().deleteById(type, id);
    }

    public DeleteResult deleteByIds(Class<?> type, Collection<?> ids, DeleteMode mode) {
        return sqlClient().deleteByIds(type, ids, mode);
    }

    public DeleteResult deleteByIds(Class<?> type, Collection<?> ids) {
        return sqlClient().deleteByIds(type, ids);
    }

    public MutableSubQuery createSubQuery(TableProxy<?> table) {
        return sqlClient().createSubQuery(table);
    }

    public <SE, ST extends TableEx<SE>, TE, TT extends TableEx<TE>> MutableSubQuery createAssociationSubQuery(AssociationTable<SE, ST, TE, TT> table) {
        return sqlClient().createAssociationSubQuery(table);
    }

    public ConnectionManager getConnectionManager() {
        return sqlClient().getConnectionManager();
    }

    public ConnectionManager getSlaveConnectionManager(boolean forUpdate) {
        return sqlClient().getSlaveConnectionManager(forUpdate);
    }

    public Dialect getDialect() {
        return sqlClient().getDialect();
    }

    public Executor getExecutor() {
        return sqlClient().getExecutor();
    }

    public EntityManager getEntityManager() {
        return sqlClient().getEntityManager();
    }

    public MetadataStrategy getMetadataStrategy() {
        return sqlClient().getMetadataStrategy();
    }

    public List<String> getExecutorContextPrefixes() {
        return sqlClient().getExecutorContextPrefixes();
    }

    public SqlFormatter getSqlFormatter() {
        return sqlClient().getSqlFormatter();
    }

    public CacheOperator getCacheOperator() {
        return sqlClient().getCacheOperator();
    }

    public TriggerType getTriggerType() {
        return sqlClient().getTriggerType();
    }

    public <T, S> ScalarProvider<T, S> getScalarProvider(Class<T> scalarType) {
        return sqlClient().getScalarProvider(scalarType);
    }

    public <T, S> ScalarProvider<T, S> getScalarProvider(TypedProp<T, ?> prop) {
        return sqlClient().getScalarProvider(prop);
    }

    public <T, S> ScalarProvider<T, S> getScalarProvider(ImmutableProp prop) {
        return sqlClient().getScalarProvider(prop);
    }

    @Override
    public ZoneId getZoneId() {
        return sqlClient().getZoneId();
    }

    public IdGenerator getIdGenerator(Class<?> entityType) {
        return sqlClient().getIdGenerator(entityType);
    }

    public int getDefaultBatchSize() {
        return sqlClient().getDefaultBatchSize();
    }

    public int getDefaultListBatchSize() {
        return sqlClient().getDefaultListBatchSize();
    }

    @Override
    public boolean isInListPaddingEnabled() {
        return sqlClient().isInListPaddingEnabled();
    }

    public int getOffsetOptimizingThreshold() {
        return sqlClient().getOffsetOptimizingThreshold();
    }

    @Override
    public LockMode getDefaultLockMode() {
        return sqlClient().getDefaultLockMode();
    }

    public TransientResolver<?, ?> getResolver(ImmutableProp prop) {
        return sqlClient().getResolver(prop);
    }

    public StrategyProvider<UserIdGenerator<?>> getUserIdGeneratorProvider() {
        return sqlClient().getUserIdGeneratorProvider();
    }

    public StrategyProvider<TransientResolver<?, ?>> getTransientResolverProvider() {
        return sqlClient().getTransientResolverProvider();
    }

    public boolean isDefaultDissociationActionCheckable() {
        return sqlClient().isDefaultDissociationActionCheckable();
    }

    public IdOnlyTargetCheckingLevel getIdOnlyTargetCheckingLevel() {
        return sqlClient().getIdOnlyTargetCheckingLevel();
    }

    public @Nullable DraftInterceptor<?, ?> getDraftInterceptor(ImmutableType type) {
        return sqlClient().getDraftInterceptor(type);
    }

    public Reader<?> getReader(Class<?> type) {
        return sqlClient().getReader(type);
    }

    public Reader<?> getReader(ImmutableType type) {
        return sqlClient().getReader(type);
    }

    public Reader<?> getReader(ImmutableProp prop) {
        return sqlClient().getReader(prop);
    }

    public String getMicroServiceName() {
        return sqlClient().getMicroServiceName();
    }

    public MicroServiceExchange getMicroServiceExchange() {
        return sqlClient().getMicroServiceExchange();
    }

    public JSqlClientImplementor caches(Consumer<CacheDisableConfig> block) {
        return sqlClient().caches(block);
    }

    public JSqlClientImplementor filters(Consumer<FilterConfig> block) {
        return sqlClient().filters(block);
    }

    public JSqlClientImplementor disableSlaveConnectionManager() {
        return sqlClient().disableSlaveConnectionManager();
    }

    public JSqlClientImplementor executor(Executor executor) {
        return sqlClient().executor(executor);
    }

    public Loaders getLoaders() {
        return sqlClient().getLoaders();
    }

    public void initialize() {
        sqlClient().initialize();
    }
}
