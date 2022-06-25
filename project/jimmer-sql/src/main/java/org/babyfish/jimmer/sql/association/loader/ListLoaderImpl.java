package org.babyfish.jimmer.sql.association.loader;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.ListLoader;
import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.ast.Executable;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.fetcher.Filter;

import java.util.Collection;
import java.util.List;
import java.util.Map;

class ListLoaderImpl<S, T> implements ListLoader<S, T> {

    private SqlClient sqlClient;

    private ImmutableProp prop;

    private Filter<?> filter;

    public ListLoaderImpl(SqlClient sqlClient, ImmutableProp prop, Filter<?> filter) {
        this.sqlClient = sqlClient;
        this.prop = prop;
        this.filter = filter;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Executable<List<T>> loadCommand(S source, int limit, int offset) {
        if (source instanceof Collection<?>) {
            throw new IllegalArgumentException(
                    "source cannot be collection, do you want to call 'batchLoadCommand'?"
            );
        }
        return new SingleCommand<>(
                sqlClient,
                prop,
                (Filter<Table<ImmutableSpi>>) filter,
                limit,
                offset,
                (ImmutableSpi) source
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public Executable<Map<S, List<T>>> batchLoadCommand(Collection<S> sources) {
        return new BatchCommand<>(
                sqlClient,
                prop,
                (Filter<Table<ImmutableSpi>>) filter,
                (Collection<ImmutableSpi>) sources
        );
    }
}
