package org.babyfish.jimmer.sql;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ast.Executable;
import org.babyfish.jimmer.sql.ast.impl.mutation.EntitiesImpl;
import org.babyfish.jimmer.sql.ast.impl.mutation.Mutations;
import org.babyfish.jimmer.sql.ast.mutation.MutableDelete;
import org.babyfish.jimmer.sql.ast.impl.query.Queries;
import org.babyfish.jimmer.sql.ast.mutation.MutableUpdate;
import org.babyfish.jimmer.sql.ast.query.ConfigurableTypedRootQuery;
import org.babyfish.jimmer.sql.ast.query.MutableRootQuery;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.TableEx;
import org.babyfish.jimmer.sql.dialect.DefaultDialect;
import org.babyfish.jimmer.sql.dialect.Dialect;
import org.babyfish.jimmer.sql.runtime.DefaultExecutor;
import org.babyfish.jimmer.sql.runtime.Executor;
import org.babyfish.jimmer.sql.runtime.ScalarProvider;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

class SqlClientImpl implements SqlClient {

    private Dialect dialect;

    private Executor executor;

    private Map<Class<?>, ScalarProvider<?, ?>> scalarProviderMap;

    private Entities entities;

    SqlClientImpl(
            Dialect dialect,
            Executor executor,
            Map<Class<?>, ScalarProvider<?, ?>> scalarProviderMap
    ) {
        this.dialect = dialect != null ? dialect : DefaultDialect.INSTANCE;
        this.executor = executor != null ? executor : DefaultExecutor.INSTANCE;
        this.scalarProviderMap = new HashMap<>(scalarProviderMap);
        this.entities = new EntitiesImpl(this);
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
    public <T extends Table<?>, R> ConfigurableTypedRootQuery<T, R> createQuery(
            Class<T> tableType,
            BiFunction<MutableRootQuery<T>, T, ConfigurableTypedRootQuery<T, R>> block
    ) {
        return Queries.createQuery(this, tableType, block);
    }

    @Override
    public <T extends TableEx<?>> Executable<Integer> createUpdate(
            Class<T> tableType,
            BiConsumer<MutableUpdate, T> block
    ) {
        return Mutations.createUpdate(this, tableType, block);
    }

    @Override
    public <T extends TableEx<?>> Executable<Integer> createDelete(
            Class<T> tableType,
            BiConsumer<MutableDelete, T> block
    ) {
        return Mutations.createDelete(this, tableType, block);
    }

    @Override
    public Entities getEntities() {
        return entities;
    }

}
