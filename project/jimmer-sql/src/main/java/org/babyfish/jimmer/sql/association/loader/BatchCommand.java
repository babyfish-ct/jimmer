package org.babyfish.jimmer.sql.association.loader;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.ast.Executable;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.fetcher.Filter;
import org.babyfish.jimmer.sql.meta.Column;

import java.sql.Connection;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;

class BatchCommand<S, T> implements Executable<Map<S, T>> {

    private SqlClient sqlClient;

    private ImmutableProp prop;

    private Filter<Table<ImmutableSpi>> filter;

    private Collection<ImmutableSpi> sources;

    public BatchCommand(
            SqlClient sqlClient,
            ImmutableProp prop,
            Filter<Table<ImmutableSpi>> filter,
            Collection<ImmutableSpi> sources
    ) {
        this.sqlClient = sqlClient;
        this.prop = prop;
        this.filter = filter;
        this.sources = sources;
    }

    @Override
    public Map<S, T> execute() {
        return sqlClient
                .getConnectionManager()
                .execute(this::execute);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<S, T> execute(Connection con) {
        return (Map<S, T>) new DataLoader(
                sqlClient,
                con,
                prop,
                filter
        ).load(sources);
    }
}
