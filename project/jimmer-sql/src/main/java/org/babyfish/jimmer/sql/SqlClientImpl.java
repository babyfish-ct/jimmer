package org.babyfish.jimmer.sql;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.association.loader.Loaders;
import org.babyfish.jimmer.sql.association.meta.AssociationType;
import org.babyfish.jimmer.sql.ast.impl.mutation.AssociationsImpl;
import org.babyfish.jimmer.sql.ast.table.AssociationTable;
import org.babyfish.jimmer.sql.cache.CacheConfig;
import org.babyfish.jimmer.sql.cache.Caches;
import org.babyfish.jimmer.sql.fetcher.Filter;
import org.babyfish.jimmer.sql.meta.IdGenerator;
import org.babyfish.jimmer.sql.ast.Executable;
import org.babyfish.jimmer.sql.ast.impl.mutation.EntitiesImpl;
import org.babyfish.jimmer.sql.ast.impl.mutation.Mutations;
import org.babyfish.jimmer.sql.ast.mutation.MutableDelete;
import org.babyfish.jimmer.sql.ast.impl.query.Queries;
import org.babyfish.jimmer.sql.ast.mutation.MutableUpdate;
import org.babyfish.jimmer.sql.ast.query.ConfigurableTypedRootQuery;
import org.babyfish.jimmer.sql.ast.query.MutableRootQuery;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.dialect.DefaultDialect;
import org.babyfish.jimmer.sql.dialect.Dialect;
import org.babyfish.jimmer.sql.runtime.*;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

class SqlClientImpl implements SqlClient {

    private static final ConnectionManager ILLEGAL_CONNECTION_MANAGER = new ConnectionManager() {
        @Override
        public <R> R execute(Function<Connection, R> block) {
            throw new ExecutionException("ConnectionManager of SqlClient is not configured");
        }
    };

    private final ConnectionManager connectionManager;

    private final Dialect dialect;

    private final Executor executor;

    private final Map<Class<?>, ScalarProvider<?, ?>> scalarProviderMap;

    private final Map<Class<?>, IdGenerator> idGeneratorMap;

    private final int defaultBatchSize;

    private final int defaultListBatchSize;

    private final Entities entities;

    private final Caches caches;

    SqlClientImpl(
            ConnectionManager connectionManager,
            Dialect dialect,
            Executor executor,
            Map<Class<?>, ScalarProvider<?, ?>> scalarProviderMap,
            Map<Class<?>, IdGenerator> idGeneratorMap,
            int defaultBatchSize,
            int defaultListBatchSize,
            Caches caches
    ) {
        this.connectionManager = connectionManager != null ?
                connectionManager :
                ILLEGAL_CONNECTION_MANAGER
        ;
        this.dialect = dialect != null ? dialect : DefaultDialect.INSTANCE;
        this.executor = executor != null ? executor : DefaultExecutor.INSTANCE;
        this.scalarProviderMap = new HashMap<>(scalarProviderMap);
        this.idGeneratorMap = new HashMap<>(idGeneratorMap);
        this.defaultBatchSize = defaultBatchSize;
        this.defaultListBatchSize = defaultListBatchSize;
        this.entities = new EntitiesImpl(this);
        this.caches = caches != null ? caches: Caches.of(cfg -> {});
    }

    @Override
    public ConnectionManager getConnectionManager() {
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
        return (ScalarProvider<T, S>) scalarProviderMap.get(scalarType);
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
    public <T extends Table<?>, R> ConfigurableTypedRootQuery<T, R> createQuery(
            Class<T> tableType,
            BiFunction<MutableRootQuery<T>, T, ConfigurableTypedRootQuery<T, R>> block
    ) {
        return Queries.createQuery(this, tableType, block);
    }

    @Override
    public <SE, ST extends Table<SE>, TE, TT extends Table<TE>, R>
    ConfigurableTypedRootQuery<AssociationTable<SE, ST, TE, TT>, R> createAssociationQuery(
            Class<ST> sourceTableType,
            Function<ST, TT> targetTableGetter,
            BiFunction<
                    MutableRootQuery<AssociationTable<SE, ST, TE, TT>>,
                    AssociationTable<SE, ST, TE, TT>,
                    ConfigurableTypedRootQuery<AssociationTable<SE, ST, TE, TT>, R>
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
        return new AssociationsImpl(this, associationType);
    }

    @Override
    public <SE, ST extends Table<SE>, TE, TT extends Table<TE>>
    ReferenceLoader<SE, TE> getReferenceLoader(
            Class<ST> sourceTableType,
            Function<ST, TT> block
    ) {
        return getReferenceLoader(sourceTableType, block, null);
    }

    @Override
    public <SE, ST extends Table<SE>, TE, TT extends Table<TE>> ReferenceLoader<SE, TE>
    getReferenceLoader(
            Class<ST> sourceTableType,
            Function<ST, TT> block,
            Filter<TT> filter
    ) {
        return Loaders.createReferenceLoader(
                this,
                ImmutableProps.join(sourceTableType, block),
                filter
        );
    }

    @Override
    public <SE, ST extends Table<SE>, TE, TT extends Table<TE>>
    ListLoader<SE, TE> getListLoader(
            Class<ST> sourceTableType,
            Function<ST, TT> block
    ) {
        return getListLoader(sourceTableType, block, null);
    }

    @Override
    public <SE, ST extends Table<SE>, TE, TT extends Table<TE>>
    ListLoader<SE, TE> getListLoader(
            Class<ST> sourceTableType,
            Function<ST, TT> block,
            Filter<TT> filter
    ) {
        return Loaders.createListLoader(
                this,
                ImmutableProps.join(sourceTableType, block),
                filter
        );
    }

    @Override
    public Caches getCaches() {
        return caches;
    }

    @Override
    public SqlClient caches(Consumer<CacheConfig> block) {
        return new SqlClientImpl(
                connectionManager,
                dialect,
                executor,
                scalarProviderMap,
                idGeneratorMap,
                defaultBatchSize,
                defaultListBatchSize,
                Caches.of(block)
        );
    }
}
