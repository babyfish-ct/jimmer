package org.babyfish.jimmer.sql.association.loader;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.ast.Executable;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.fetcher.Filter;

import java.sql.Connection;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

class SingleCommand<T> implements Executable<T> {

    private SqlClient sqlClient;

    private ImmutableProp prop;

    private Filter<Table<ImmutableSpi>> filter;

    private int limit;

    private int offset;

    private ImmutableSpi source;

    public SingleCommand(
            SqlClient sqlClient,
            ImmutableProp prop,
            Filter<Table<ImmutableSpi>> filter,
            int limit,
            int offset,
            ImmutableSpi source
    ) {
        this.sqlClient = sqlClient;
        this.prop = prop;
        this.filter = filter;
        this.limit = limit;
        this.offset = offset;
        this.source = source;
    }

    @Override
    public T execute() {
        return sqlClient
                .getConnectionManager()
                .execute(this::executeImpl);
    }

    @Override
    public T execute(Connection con) {
        if (con != null) {
            return executeImpl(con);
        }
        return sqlClient
                .getConnectionManager()
                .execute(this::executeImpl);
    }

    @SuppressWarnings("unchecked")
    private T executeImpl(Connection con) {
        return (T) new DataLoader(
                sqlClient,
                con,
                prop,
                filter,
                limit,
                offset
        ).load(Collections.singleton(source)).get(source);
    }
}
