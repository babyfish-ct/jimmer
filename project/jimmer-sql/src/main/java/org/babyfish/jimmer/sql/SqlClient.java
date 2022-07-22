package org.babyfish.jimmer.sql;

import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.association.meta.AssociationType;
import org.babyfish.jimmer.sql.ast.table.AssociationTable;
import org.babyfish.jimmer.sql.cache.CacheConfig;
import org.babyfish.jimmer.sql.cache.Caches;
import org.babyfish.jimmer.sql.fetcher.Filter;
import org.babyfish.jimmer.sql.meta.IdGenerator;
import org.babyfish.jimmer.sql.ast.Executable;
import org.babyfish.jimmer.sql.ast.mutation.MutableDelete;
import org.babyfish.jimmer.sql.ast.mutation.MutableUpdate;
import org.babyfish.jimmer.sql.ast.query.ConfigurableTypedRootQuery;
import org.babyfish.jimmer.sql.ast.query.MutableRootQuery;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.dialect.Dialect;
import org.babyfish.jimmer.sql.runtime.ConnectionManager;
import org.babyfish.jimmer.sql.runtime.Executor;
import org.babyfish.jimmer.sql.runtime.ScalarProvider;
import org.babyfish.jimmer.sql.runtime.SqlClientImpl;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public interface SqlClient {

    static Builder newBuilder() {
        return new SqlClientImpl.BuilderImpl();
    }

    ConnectionManager getConnectionManager();

    Dialect getDialect();

    Executor getExecutor();

    <T, S> ScalarProvider<T, S> getScalarProvider(Class<T> scalarType);

    IdGenerator getIdGenerator(Class<?> entityType);

    int getDefaultBatchSize();

    int getDefaultListBatchSize();

    <T extends Table<?>, R> ConfigurableTypedRootQuery<T, R> createQuery(
            Class<T> tableType,
            BiFunction<MutableRootQuery<T>, T, ConfigurableTypedRootQuery<T, R>> block
    );

    <SE, ST extends Table<SE>, TE, TT extends Table<TE>, R>
    ConfigurableTypedRootQuery<AssociationTable<SE, ST, TE, TT>, R> createAssociationQuery(
            Class<ST> sourceTableType,
            Function<ST, TT> targetTableGetter,
            BiFunction<
                    MutableRootQuery<AssociationTable<SE, ST, TE, TT>>,
                    AssociationTable<SE, ST, TE, TT>,
                    ConfigurableTypedRootQuery<AssociationTable<SE, ST, TE, TT>, R>
            > block
    );

    <T extends Table<?>> Executable<Integer> createUpdate(
            Class<T> tableType,
            BiConsumer<MutableUpdate, T> block
    );

    <T extends Table<?>> Executable<Integer> createDelete(
            Class<T> tableType,
            BiConsumer<MutableDelete, T> block
    );

    Entities getEntities();

    Triggers getTriggers();

    <ST extends Table<?>> Associations getAssociations(
            Class<ST> sourceTableType,
            Function<ST, ? extends Table<?>> block
    );

    Associations getAssociations(Class<?> entityType, String prop);

    Associations getAssociations(ImmutableProp immutableProp);

    Associations getAssociations(AssociationType associationType);

    <SE, ST extends Table<SE>, TE, TT extends Table<TE>>
    ReferenceLoader<SE, TE> getReferenceLoader(
            Class<ST> sourceTableType,
            Function<ST, TT> block
    );

    <SE, ST extends Table<SE>, TE, TT extends Table<TE>>
    ReferenceLoader<SE, TE> getReferenceLoader(
            Class<ST> sourceTableType,
            Function<ST, TT> block,
            Filter<TT> filter
    );

    <SE, ST extends Table<SE>, TE, TT extends Table<TE>>
    ListLoader<SE, TE> getListLoader(
            Class<ST> sourceTableType,
            Function<ST, TT> block
    );

    <SE, ST extends Table<SE>, TE, TT extends Table<TE>>
    ListLoader<SE, TE> getListLoader(
            Class<ST> sourceTableType,
            Function<ST, TT> block,
            Filter<TT> filter
    );

    Caches getCaches();

    SqlClient caches(Consumer<CacheConfig> block);

    interface Builder {

        @OldChain
        Builder setConnectionManager(ConnectionManager connectionManager);

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
        Builder setCaches(Consumer<CacheConfig> block);

        SqlClient build();
    }
}
