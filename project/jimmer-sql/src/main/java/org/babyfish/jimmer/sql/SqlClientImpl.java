package org.babyfish.jimmer.sql;

import org.babyfish.jimmer.sql.dialect.DefaultDialect;
import org.babyfish.jimmer.sql.dialect.Dialect;
import org.babyfish.jimmer.sql.runtime.DefaultExecutor;
import org.babyfish.jimmer.sql.runtime.Executor;
import org.babyfish.jimmer.sql.runtime.ScalarProvider;

import java.util.HashMap;
import java.util.Map;

class SqlClientImpl implements SqlClient {

    private Dialect dialect;

    private Executor executor;

    private Map<Class<?>, ScalarProvider<?, ?>> scalarProviderMap;

    SqlClientImpl(
            Dialect dialect,
            Executor executor,
            Map<Class<?>, ScalarProvider<?, ?>> scalarProviderMap
    ) {
        this.dialect = dialect != null ? dialect : DefaultDialect.INSTANCE;
        this.executor = executor != null ? executor : DefaultExecutor.INSTANCE;
        this.scalarProviderMap = new HashMap<>(scalarProviderMap);
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
}
