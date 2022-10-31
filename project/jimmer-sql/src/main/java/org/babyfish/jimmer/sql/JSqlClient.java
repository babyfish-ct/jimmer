package org.babyfish.jimmer.sql;

import org.babyfish.jimmer.lang.NewChain;
import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.association.meta.AssociationType;
import org.babyfish.jimmer.sql.ast.query.MutableQuery;
import org.babyfish.jimmer.sql.ast.query.MutableSubQuery;
import org.babyfish.jimmer.sql.ast.table.AssociationTable;
import org.babyfish.jimmer.sql.ast.table.TableEx;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;
import org.babyfish.jimmer.sql.cache.CacheConfig;
import org.babyfish.jimmer.sql.cache.CacheDisableConfig;
import org.babyfish.jimmer.sql.cache.Caches;
import org.babyfish.jimmer.sql.filter.Filter;
import org.babyfish.jimmer.sql.filter.FilterConfig;
import org.babyfish.jimmer.sql.filter.Filters;
import org.babyfish.jimmer.sql.loader.Loaders;
import org.babyfish.jimmer.sql.meta.IdGenerator;
import org.babyfish.jimmer.sql.ast.Executable;
import org.babyfish.jimmer.sql.ast.mutation.MutableDelete;
import org.babyfish.jimmer.sql.ast.mutation.MutableUpdate;
import org.babyfish.jimmer.sql.ast.query.ConfigurableRootQuery;
import org.babyfish.jimmer.sql.ast.query.MutableRootQuery;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.dialect.Dialect;
import org.babyfish.jimmer.sql.runtime.ConnectionManager;
import org.babyfish.jimmer.sql.runtime.EntityManager;
import org.babyfish.jimmer.sql.runtime.Executor;
import org.babyfish.jimmer.sql.runtime.ScalarProvider;

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public interface JSqlClient {

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

    /**
     * Create fluent style query
     * @param table
     * @return
     */
    <T extends TableProxy<?>> MutableRootQuery<T> createQuery(T table);

    /**
     * Create fluent style sub query
     * @param table
     * @return
     */
    MutableSubQuery createSubQuery(TableProxy<?> table);

    /**
     * Create fluent style-update
     * @param table
     * @return
     */
    MutableUpdate createUpdate(TableProxy<?> table);

    /**
     * Create fluent style delete
     * @param table
     * @return
     */
    MutableDelete createDelete(TableProxy<?> table);

    /**
     * create labmda style query
     * @param tableType
     * @param block
     * @param <T>
     * @param <R>
     * @return
     */
    <T extends Table<?>, R> ConfigurableRootQuery<T, R> createQuery(
            Class<T> tableType,
            BiFunction<MutableRootQuery<T>, T, ConfigurableRootQuery<T, R>> block
    );

    /**
     * Create lambda style association query
     * @param sourceTableType
     * @param targetTableGetter
     * @param block
     * @param <SE>
     * @param <ST>
     * @param <TE>
     * @param <TT>
     * @param <R>
     * @return
     */
    <SE, ST extends Table<SE>, TE, TT extends Table<TE>, R>
    ConfigurableRootQuery<AssociationTable<SE, ST, TE, TT>, R> createAssociationQuery(
            Class<ST> sourceTableType,
            Function<ST, TT> targetTableGetter,
            BiFunction<
                    MutableRootQuery<AssociationTable<SE, ST, TE, TT>>,
                    AssociationTable<SE, ST, TE, TT>,
                    ConfigurableRootQuery<AssociationTable<SE, ST, TE, TT>, R>
            > block
    );

    /**
     * Create lambda style update
     * @param tableType
     * @param block
     * @param <T>
     * @return
     */
    <T extends Table<?>> Executable<Integer> createUpdate(
            Class<T> tableType,
            BiConsumer<MutableUpdate, T> block
    );

    /**
     * Create lambda style delete
     * @param tableType
     * @param block
     * @param <T>
     * @return
     */
    <T extends Table<?>> Executable<Integer> createDelete(
            Class<T> tableType,
            BiConsumer<MutableDelete, T> block
    );

    Entities getEntities();

    Triggers getTriggers();

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

        JSqlClient build();
    }
}
