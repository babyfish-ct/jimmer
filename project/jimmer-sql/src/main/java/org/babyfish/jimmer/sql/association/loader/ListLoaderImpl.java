package org.babyfish.jimmer.sql.association.loader;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.ListLoader;
import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.ast.Executable;
import org.babyfish.jimmer.sql.ast.query.Sortable;
import org.babyfish.jimmer.sql.ast.table.Table;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

class ListLoaderImpl<S, T> implements ListLoader<S, T> {

    private SqlClient sqlClient;

    private ImmutableProp prop;

    private BiConsumer<Sortable, Table<?>> filter;

    public ListLoaderImpl(SqlClient sqlClient, ImmutableProp prop, BiConsumer<Sortable, Table<?>> filter) {
        this.sqlClient = sqlClient;
        this.prop = prop;
        this.filter = filter;
    }

    @Override
    public Executable<List<T>> loadCommand(S source, int limit, int offset) {
        return new SingleCommand<>(
                sqlClient,
                prop,
                filter,
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
                filter,
                (Collection<ImmutableSpi>) sources
        );
    }
}
