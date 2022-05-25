package org.babyfish.jimmer.sql.association.loader;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.ast.Executable;
import org.babyfish.jimmer.sql.ast.query.Sortable;
import org.babyfish.jimmer.sql.ast.table.Table;

import java.sql.Connection;
import java.util.function.BiConsumer;

class SingleCommand<T> implements Executable<T> {

    private SqlClient sqlClient;

    private ImmutableProp prop;

    private BiConsumer<Sortable, Table<?>> filter;

    private int limit;

    private int offset;

    private ImmutableSpi source;

    public SingleCommand(
            SqlClient sqlClient,
            ImmutableProp prop,
            BiConsumer<Sortable, Table<?>> filter,
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
                .execute(this::execute);
    }

    @SuppressWarnings("unchecked")
    @Override
    public T execute(Connection con) {
        return (T) new SingleDataLoader(
                sqlClient,
                con,
                prop,
                filter,
                limit,
                offset
        ).load(Keys.key(prop, source));
    }
}
