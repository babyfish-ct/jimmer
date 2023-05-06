package org.babyfish.jimmer.sql.loader.graphql.impl;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.ast.Executable;
import org.babyfish.jimmer.sql.loader.graphql.ValueLoader;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;

import java.sql.Connection;
import java.util.Collection;
import java.util.Map;

class ValueLoaderImpl<S, V> implements ValueLoader<S, V> {

    private final JSqlClientImplementor sqlClient;

    private final Connection con;

    private final ImmutableProp prop;

    public ValueLoaderImpl(
            JSqlClientImplementor sqlClient,
            ImmutableProp prop
    ) {
        this(sqlClient, null, prop);
    }

    public ValueLoaderImpl(
            JSqlClientImplementor sqlClient,
            Connection con,
            ImmutableProp prop
    ) {
        this.sqlClient = sqlClient;
        this.con = con;
        this.prop = prop;
    }

    @Override
    public ValueLoader<S, V> forConnection(Connection con) {
        if (this.con == con) {
            return this;
        }
        return new ValueLoaderImpl<>(sqlClient, con, prop);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Executable<V> loadCommand(S source) {
        if (source instanceof Collection<?>) {
            throw new IllegalArgumentException(
                    "source cannot be collection, do you want to call 'batchLoadCommand'?"
            );
        }
        return new SingleCommand<>(
                sqlClient,
                con,
                prop,
                null,
                Integer.MAX_VALUE,
                0,
                (ImmutableSpi) source,
                null
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public Executable<Map<S, V>> batchLoadCommand(Collection<S> sources) {
        return new BatchCommand<>(
                sqlClient,
                con,
                prop,
                null,
                (Collection<ImmutableSpi>) sources,
                null
        );
    }
}
