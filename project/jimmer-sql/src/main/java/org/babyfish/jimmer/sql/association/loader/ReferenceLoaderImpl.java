package org.babyfish.jimmer.sql.association.loader;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.ReferenceLoader;
import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.ast.Executable;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.fetcher.Filter;

import java.util.Collection;
import java.util.Map;

class ReferenceLoaderImpl<S, T> implements ReferenceLoader<S, T> {

    private SqlClient sqlClient;

    private ImmutableProp prop;

    private Filter<?> filter;

    public ReferenceLoaderImpl(
            SqlClient sqlClient,
            ImmutableProp prop,
            Filter<?> filter
    ) {
        this.sqlClient = sqlClient;
        this.prop = prop;
        this.filter = filter;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Executable<T> loadCommand(S source) {
        if (source instanceof Collection<?>) {
            throw new IllegalArgumentException(
                    "source cannot be collection, do you want to call 'batchLoadCommand'?"
            );
        }
        return new SingleCommand<>(
                sqlClient,
                prop,
                (Filter<Table<ImmutableSpi>>) filter,
                Integer.MAX_VALUE,
                0,
                (ImmutableSpi) source
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public Executable<Map<S, T>> batchLoadCommand(Collection<S> sources) {
        return new BatchCommand<>(
                sqlClient,
                prop,
                (Filter<Table<ImmutableSpi>>) filter,
                (Collection<ImmutableSpi>) sources
        );
    }
}
