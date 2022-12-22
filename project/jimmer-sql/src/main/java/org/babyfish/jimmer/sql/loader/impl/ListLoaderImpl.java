package org.babyfish.jimmer.sql.loader.impl;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.loader.FilterableListLoader;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.Executable;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.fetcher.FieldFilter;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

class ListLoaderImpl<SE, TE, TT extends Table<TE>> implements FilterableListLoader<SE, TE, TT> {

    private final JSqlClient sqlClient;

    private Connection con;

    private final ImmutableProp prop;

    private final FieldFilter<?> filter;

    public ListLoaderImpl(JSqlClient sqlClient, ImmutableProp prop) {
        this(sqlClient, null, prop, null);
    }

    private ListLoaderImpl(
            JSqlClient sqlClient,
            Connection con,
            ImmutableProp prop,
            FieldFilter<?> filter
    ) {
        this.sqlClient = sqlClient;
        this.con = con;
        this.prop = prop;
        this.filter = filter;
    }

    @Override
    public FilterableListLoader<SE, TE, TT> forConnection(Connection con) {
        if (this.con == con) {
            return this;
        }
        return new ListLoaderImpl<>(sqlClient, con, prop, filter);
    }

    @Override
    public FilterableListLoader<SE, TE, TT> forFilter(FieldFilter<TT> filter) {
        if (this.filter == filter) {
            return this;
        }
        return new ListLoaderImpl<>(sqlClient, con, prop, filter);
    }

    @NotNull
    @SuppressWarnings("unchecked")
    @Override
    public Executable<List<TE>> loadCommand(@NotNull SE source, int limit, int offset) {
        if (source instanceof Collection<?>) {
            throw new IllegalArgumentException(
                    "source cannot be collection, do you want to call 'batchLoadCommand'?"
            );
        }
        return new SingleCommand<>(
                sqlClient,
                con,
                prop,
                (FieldFilter<Table<ImmutableSpi>>) filter,
                limit,
                offset,
                (ImmutableSpi) source,
                Collections.emptyList()
        );
    }

    @NotNull
    @SuppressWarnings("unchecked")
    @Override
    public Executable<Map<SE, List<TE>>> batchLoadCommand(@NotNull Collection<SE> sources) {
        return new BatchCommand<>(
                sqlClient,
                con,
                prop,
                (FieldFilter<Table<ImmutableSpi>>) filter,
                (Collection<ImmutableSpi>) sources,
                Collections.emptyList()
        );
    }
}
