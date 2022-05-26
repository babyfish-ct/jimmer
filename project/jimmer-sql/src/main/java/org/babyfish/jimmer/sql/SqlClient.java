package org.babyfish.jimmer.sql;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.association.meta.AssociationType;
import org.babyfish.jimmer.sql.ast.query.Sortable;
import org.babyfish.jimmer.sql.ast.table.AssociationTable;
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

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface SqlClient {

    static Builder newBuilder() {
        return new Builder();
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
            BiConsumer<Sortable, TT> filter
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
            BiConsumer<Sortable, TT> filter
    );

    class Builder {

        private ConnectionManager connectionManager;

        private Dialect dialect;

        private Executor executor;

        private final Map<Class<?>, ScalarProvider<?, ?>> scalarProviderMap = new HashMap<>();

        private final Map<Class<?>, IdGenerator> idGeneratorMap = new HashMap<>();

        private int defaultBatchSize = 128;

        private int defaultListBatchSize = 16;

        Builder() {}

        public Builder setConnectionManager(ConnectionManager connectionManager) {
            this.connectionManager = connectionManager;
            return this;
        }

        public Builder setDialect(Dialect dialect) {
            this.dialect = dialect;
            return this;
        }

        public Builder setExecutor(Executor executor) {
            this.executor = executor;
            return this;
        }

        public Builder setIdGenerator(IdGenerator idGenerator) {
            return setIdGenerator(null, idGenerator);
        }

        public Builder setIdGenerator(Class<?> entityType, IdGenerator idGenerator) {
            idGeneratorMap.put(entityType, idGenerator);
            return this;
        }

        public Builder addScalarProvider(ScalarProvider<?, ?> scalarProvider) {
            if (scalarProviderMap.containsKey(scalarProvider.getScalarType())) {
                throw new IllegalStateException(
                        "Cannot set scalar provider for scalar type \"" +
                                scalarProvider.getScalarType() +
                                "\" twice"
                );
            }
            scalarProviderMap.put(scalarProvider.getScalarType(), scalarProvider);
            return this;
        }

        public Builder setDefaultBatchSize(int size) {
            if (size < 1) {
                throw new IllegalStateException("size cannot be less than 1");
            }
            defaultBatchSize = size;
            return this;
        }

        public Builder setDefaultListBatchSize(int size) {
            if (size < 1) {
                throw new IllegalStateException("size cannot be less than 1");
            }
            defaultListBatchSize = size;
            return this;
        }

        public SqlClient build() {
            return new SqlClientImpl(
                    connectionManager,
                    dialect,
                    executor,
                    scalarProviderMap,
                    idGeneratorMap,
                    defaultBatchSize,
                    defaultListBatchSize
            );
        }
    }
}
