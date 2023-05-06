package org.babyfish.jimmer.sql;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.babyfish.jimmer.lang.NewChain;
import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.association.meta.AssociationType;
import org.babyfish.jimmer.sql.ast.mutation.*;
import org.babyfish.jimmer.sql.ast.query.*;
import org.babyfish.jimmer.sql.ast.table.AssociationTable;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;
import org.babyfish.jimmer.sql.cache.CacheConfig;
import org.babyfish.jimmer.sql.cache.CacheDisableConfig;
import org.babyfish.jimmer.sql.cache.Caches;
import org.babyfish.jimmer.sql.event.TriggerType;
import org.babyfish.jimmer.sql.event.Triggers;
import org.babyfish.jimmer.sql.event.binlog.BinLog;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.filter.Filter;
import org.babyfish.jimmer.sql.filter.FilterConfig;
import org.babyfish.jimmer.sql.filter.Filters;
import org.babyfish.jimmer.sql.loader.graphql.Loaders;
import org.babyfish.jimmer.sql.meta.DatabaseNamingStrategy;
import org.babyfish.jimmer.sql.meta.IdGenerator;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.dialect.Dialect;
import org.babyfish.jimmer.sql.runtime.*;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;

public interface JSqlClient extends SubQueryProvider {

    static Builder newBuilder() {
        return new JSqlClientImpl.BuilderImpl();
    }

    <T extends TableProxy<?>> MutableRootQuery<T> createQuery(T table);

    MutableUpdate createUpdate(TableProxy<?> table);

    MutableDelete createDelete(TableProxy<?> table);

    <SE, ST extends Table<SE>, TE, TT extends Table<TE>>
    MutableRootQuery<AssociationTable<SE, ST, TE, TT>> createAssociationQuery(
            AssociationTable<SE,ST, TE, TT> table
    );

    Entities getEntities();

    /**
     * This method is equivalent to `getTriggers(false)`
     * @return
     */
    Triggers getTriggers();

    /**
     * <ul>
     *     <li>
     *         If trigger type is 'BINLOG_ONLY'
     *         <ul>
     *             <li>If `transaction` is true, throws exception</li>
     *             <li>If `transaction` is false, return binlog trigger</li>
     *         </ul>
     *     </li>
     *     <li>
     *         If trigger type is 'TRANSACTION_ONLY', returns transaction trigger
     *         no matter what the `transaction` is
     *     </li>
     *     <li>
     *         If trigger type is 'BOTH'
     *         <ul>
     *             <li>If `transaction` is true, return transaction trigger</li>
     *             <li>If `transaction` is false, return binlog trigger</li>
     *         </ul>
     *         Note that the objects returned by different parameters are independent of each other.
     *     </li>
     * </ul>
     * @param transaction
     * @return Trigger
     */
    Triggers getTriggers(boolean transaction);

    Associations getAssociations(TypedProp.Association<?, ?> prop);

    Associations getAssociations(ImmutableProp immutableProp);

    Associations getAssociations(AssociationType associationType);

    Loaders getLoaders();

    Caches getCaches();

    Filters getFilters();

    BinLog getBinLog();

    @NewChain
    JSqlClient caches(Consumer<CacheDisableConfig> block);

    @NewChain
    JSqlClient filters(Consumer<FilterConfig> block);

    @NewChain
    JSqlClient disableSlaveConnectionManager();

    @Nullable
    default <E> E findById(Class<E> entityType, Object id) {
        return getEntities().findById(entityType, id);
    }

    @Nullable
    default <E> E findById(Fetcher<E> fetcher, Object id) {
        return getEntities().findById(fetcher, id);
    }

    @Nullable
    default <K, V> Map<K, V> findByIds(Class<V> entityType, Collection<K> ids) {
        return getEntities().findMapByIds(entityType, ids);
    }

    @Nullable
    default <K, V> Map<K, V> findByIds(Fetcher<V> entityType, Collection<K> ids) {
        return getEntities().findMapByIds(entityType, ids);
    }

    default <E> SimpleSaveResult<E> save(E entity, SaveMode mode) {
        return getEntities().saveCommand(entity)
                .setMode(mode)
                .setAutoAttachingAll()
                .execute();
    }

    default <E> SimpleSaveResult<E> save(E entity) {
        return save(entity, SaveMode.UPSERT);
    }

    default <E> SimpleSaveResult<E> insert(E entity) {
        return save(entity, SaveMode.INSERT_ONLY);
    }

    default <E> SimpleSaveResult<E> update(E entity) {
        return save(entity, SaveMode.UPDATE_ONLY);
    }

    default DeleteResult deleteById(Class<?> entityType, Object id, DeleteMode mode) {
        return getEntities().delete(entityType, id, mode);
    }

    default DeleteResult deleteById(Class<?> entityType, Object id) {
        return getEntities().delete(entityType, id, DeleteMode.AUTO);
    }

    default DeleteResult deleteByIds(Class<?> entityType, Collection<?> ids, DeleteMode mode) {
        return getEntities().batchDelete(entityType, ids, mode);
    }

    default DeleteResult deleteByIds(Class<?> entityType, Collection<?> ids) {
        return getEntities().batchDelete(entityType, ids, DeleteMode.AUTO);
    }

    interface Builder {

        int DEFAULT_BATCH_SIZE = 128;

        int DEFAULT_LIST_BATCH_SIZE = 16;

        @OldChain
        Builder setConnectionManager(ConnectionManager connectionManager);

        @OldChain
        Builder setSlaveConnectionManager(ConnectionManager connectionManager);

        @OldChain
        Builder setDialect(Dialect dialect);

        @OldChain
        Builder setExecutor(Executor executor);

        /**
         * If this option is configured, when jimmer calls back
         * `org.babyfish.jimmer.sql.runtime.Executor.execute` before executing SQL,
         * it will check the stack trace information of the current thread.
         *
         * However, these stack traces have too much information, including
         * infrastructure call frames represented by jdk, jdbc driver, jimmer, and spring,
         * and the business-related information you care about will be submerged in the ocean of information.
         *
         * Through this configuration, you can specify multiple package or class prefixes, and jimmer will
         * judge whether there are some call frames in the stack trace whose class names start with some
         * of these prefixes. If the judgment is true, jimmer believes that the current callback is related
         * to your business, and the `ctx` parameter of `org.babyfish.jimmer.sql.runtime.Executor.execute`
         * will be passed as non-null.
         *
         * If the SQL logging configuration is enabled at the same time, when a SQL statement is caused by
         * the business you care about, the business call frame will be printed together with the SQL log.
         */
        @OldChain
        Builder setExecutorContextPrefixes(Collection<String> prefixes);

        @OldChain
        Builder setTransientResolverProvider(TransientResolverProvider transientResolverProvider);

        @OldChain
        Builder setIdGenerator(IdGenerator idGenerator);

        @OldChain
        Builder setIdGenerator(Class<?> entityType, IdGenerator idGenerator);

        @OldChain
        Builder addScalarProvider(ScalarProvider<?, ?> scalarProvider);

        @OldChain
        Builder addScalarProvider(TypedProp<?, ?> prop, ScalarProvider<?, ?> scalarProvider);

        @OldChain
        Builder addScalarProvider(ImmutableProp prop, ScalarProvider<?, ?> scalarProvider);

        @OldChain
        Builder setDefaultEnumStrategy(EnumType.Strategy strategy);

        @OldChain
        Builder setDatabaseNamingStrategy(DatabaseNamingStrategy strategy);

        @OldChain
        Builder setDefaultBatchSize(int size);

        @OldChain
        Builder setDefaultListBatchSize(int size);

        /**
         * For RDBMS, pagination is slow if `offset` is large, especially for MySQL.
         *
         * If `offset` >= $thisArgument
         *
         * <pre>{@code
         *  select t.* from Table t ... limit ? offset ?
         * }</pre>
         *
         * will be automatically changed to
         *
         * <pre>{@code
         *  select t.* from (
         *      select
         *          t.id as optimized_core_id_
         *      from Table t ... limit ? offset ?
         *  ) optimized_core_
         *  inner join Table as optimized_
         *      on optimized_.optimized_core_id_ = optimized_core_.optimized_core_id_
         * }</pre>
         *
         * @return An integer which is greater than 0
         */
        @OldChain
        Builder setOffsetOptimizingThreshold(int threshold);

        /**
         * Under normal circumstances, users do not need to set the entity manager.
         *
         * This configuration is for compatibility with version 0.7.47 and earlier.
         */
        @OldChain
        Builder setEntityManager(EntityManager entityManager);

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
        Builder ignoreBuiltInFilters();

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

        @OldChain
        Builder setForeignKeyEnabledByDefault(boolean enabled);

        @OldChain
        Builder addCustomizers(Customizer ... customizers);

        @OldChain
        Builder addCustomizers(Collection<Customizer> customizers);

        @OldChain
        Builder addInitializers(Initializer ... initializers);

        @OldChain
        Builder addInitializers(Collection<Initializer> initializers);

        @OldChain
        Builder setDatabaseValidationMode(DatabaseValidationMode mode);

        @OldChain
        Builder setDatabaseValidationCatalog(String catalog);

        @OldChain
        Builder setMicroServiceName(String microServiceName);

        @OldChain
        Builder setMicroServiceExchange(MicroServiceExchange exchange);

        JSqlClient build();
    }
}
