package org.babyfish.jimmer.sql.loader.impl;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.Executable;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.fetcher.FieldFilter;

import java.sql.Connection;
import java.util.Collections;

class SingleCommand<T> implements Executable<T> {

    private final JSqlClient sqlClient;

    private final Connection con;

    private final ImmutableProp prop;

    private final FieldFilter<Table<ImmutableSpi>> filter;

    private final int limit;

    private final int offset;

    private final ImmutableSpi source;

    private final T defaultValue;

    public SingleCommand(
            JSqlClient sqlClient,
            Connection con,
            ImmutableProp prop,
            FieldFilter<Table<ImmutableSpi>> filter,
            int limit,
            int offset,
            ImmutableSpi source,
            T defaultValue
    ) {
        this.sqlClient = sqlClient;
        this.con = con;
        this.prop = prop;
        this.filter = filter;
        this.limit = limit;
        this.offset = offset;
        this.source = source;
        this.defaultValue = defaultValue;
    }

    @Override
    public T execute() {
        if (con != null) {
            return executeImpl(con);
        }
        return sqlClient
                .getConnectionManager()
                .execute(this::executeImpl);
    }

    @Override
    public T execute(Connection con) {
        if (con != null) {
            return executeImpl(con);
        }
        if (this.con != null) {
            return executeImpl(this.con);
        }
        return sqlClient
                .getConnectionManager()
                .execute(this::executeImpl);
    }

    @SuppressWarnings("unchecked")
    private T executeImpl(Connection con) {
        T result = (T) new DataLoader(
                sqlClient,
                con,
                prop,
                filter,
                limit,
                offset
        ).load(Collections.singleton(source)).get(source);
        return result != null ? result : defaultValue;
    }
}
