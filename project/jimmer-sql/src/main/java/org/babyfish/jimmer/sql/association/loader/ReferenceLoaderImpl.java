package org.babyfish.jimmer.sql.association.loader;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.ReferenceLoader;
import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.ast.Executable;
import org.babyfish.jimmer.sql.ast.query.Sortable;
import org.babyfish.jimmer.sql.ast.table.Table;

import java.util.Collection;
import java.util.Map;
import java.util.function.BiConsumer;

class ReferenceLoaderImpl<S, T> implements ReferenceLoader<S, T> {

    private SqlClient sqlClient;

    private ImmutableProp prop;

    private BiConsumer<Sortable, Table<?>> filter;

    public ReferenceLoaderImpl(
            SqlClient sqlClient,
            ImmutableProp prop,
            BiConsumer<Sortable, Table<?>> filter
    ) {
        this.sqlClient = sqlClient;
        this.prop = prop;
        this.filter = filter;
    }

    @Override
    public Executable<T> loadCommand(S source) {
        return new SingleCommand<>(
                sqlClient,
                prop,
                filter,
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
                filter,
                (Collection<ImmutableSpi>) sources
        );
    }
}
