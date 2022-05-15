package org.babyfish.jimmer.sql;

import org.babyfish.jimmer.meta.sql.IdGenerator;
import org.babyfish.jimmer.meta.sql.UserIdGenerator;
import org.babyfish.jimmer.sql.ast.Executable;
import org.babyfish.jimmer.sql.ast.mutation.MutableDelete;
import org.babyfish.jimmer.sql.ast.mutation.MutableUpdate;
import org.babyfish.jimmer.sql.ast.query.ConfigurableTypedRootQuery;
import org.babyfish.jimmer.sql.ast.query.MutableRootQuery;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.TableEx;
import org.babyfish.jimmer.sql.dialect.Dialect;
import org.babyfish.jimmer.sql.runtime.Executor;
import org.babyfish.jimmer.sql.runtime.ScalarProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public interface SqlClient {

    static Builder newBuilder() {
        return new Builder();
    }

    Dialect getDialect();

    Executor getExecutor();

    <T, S> ScalarProvider<T, S> getScalarProvider(Class<T> scalarType);

    IdGenerator getIdGenerator(Class<?> entityType);

    <T extends Table<?>, R> ConfigurableTypedRootQuery<T, R> createQuery(
            Class<T> tableType,
            BiFunction<MutableRootQuery<T>, T, ConfigurableTypedRootQuery<T, R>> block
    );

    <T extends TableEx<?>> Executable<Integer> createUpdate(
            Class<T> tableType,
            BiConsumer<MutableUpdate, T> block
    );

    <T extends TableEx<?>> Executable<Integer> createDelete(
            Class<T> tableType,
            BiConsumer<MutableDelete, T> block
    );

    Entities getEntities();

    class Builder {

        private Dialect dialect;

        private Executor executor;

        private Map<Class<?>, ScalarProvider<?, ?>> scalarProviderMap = new HashMap<>();

        private Map<Class<?>, UserIdGenerator> userIdGeneratorMap = new HashMap<>();

        Builder() {}

        public Builder setDialect(Dialect dialect) {
            this.dialect = dialect;
            return this;
        }

        public Builder setExecutor(Executor executor) {
            this.executor = executor;
            return this;
        }

        public Builder setUserIdGenerator(UserIdGenerator userIdGenerator) {
            return setUserIdGenerator(null, userIdGenerator);
        }

        public Builder setUserIdGenerator(Class<?> entityType, UserIdGenerator userIdGenerator) {
            userIdGeneratorMap.put(entityType, userIdGenerator);
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

        public SqlClient build() {
            return new SqlClientImpl(
                    dialect,
                    executor,
                    scalarProviderMap,
                    userIdGeneratorMap
            );
        }
    }
}
