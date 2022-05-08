package org.babyfish.jimmer.sql;

import org.babyfish.jimmer.sql.runtime.DefaultExecutor;
import org.babyfish.jimmer.sql.runtime.Executor;
import org.babyfish.jimmer.sql.runtime.ScalarProvider;

import java.util.HashMap;
import java.util.Map;

class SqlClientImpl implements SqlClient {

    private Executor executor;

    private Map<Class<?>, ScalarProvider<?, ?>> scalarProviderMap;

    SqlClientImpl(
            Executor executor,
            Map<Class<?>, ScalarProvider<?, ?>> scalarProviderMap
    ) {
        this.executor = executor != null ? executor : DefaultExecutor.INSTANCE;
        this.scalarProviderMap = new HashMap<>(scalarProviderMap);
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
