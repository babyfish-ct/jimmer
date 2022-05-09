package org.babyfish.jimmer.sql;

import org.babyfish.jimmer.sql.dialect.Dialect;
import org.babyfish.jimmer.sql.runtime.Executor;
import org.babyfish.jimmer.sql.runtime.ScalarProvider;

import java.util.HashMap;
import java.util.Map;

public interface SqlClient {

    static Builder newBuilder() {
        return new Builder();
    }

    Dialect getDialect();

    Executor getExecutor();

    <T, S> ScalarProvider<T, S> getScalarProvider(Class<T> scalarType);

    class Builder {

        private Dialect dialect;

        private Executor executor;

        private Map<Class<?>, ScalarProvider<?, ?>> scalarProviderMap = new HashMap<>();

        Builder() {}

        public Builder setDialect(Dialect dialect) {
            this.dialect = dialect;
            return this;
        }

        public Builder setExecutor(Executor executor) {
            this.executor = executor;
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
            return new SqlClientImpl(dialect, executor, scalarProviderMap);
        }
    }
}
