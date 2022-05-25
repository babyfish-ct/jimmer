package org.babyfish.jimmer.sql.association.loader;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.ast.Executable;
import org.babyfish.jimmer.sql.ast.query.Sortable;
import org.babyfish.jimmer.sql.ast.table.Table;

import java.sql.Connection;
import java.util.Collection;
import java.util.Map;
import java.util.function.BiConsumer;

class BatchCommand<S, T> implements Executable<Map<S, T>> {

    private SqlClient sqlClient;

    private ImmutableProp prop;

    private BiConsumer<Sortable, Table<?>> filter;

    private Collection<ImmutableSpi> sources;

    public BatchCommand(
            SqlClient sqlClient,
            ImmutableProp prop,
            BiConsumer<Sortable, Table<?>> filter,
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
        Map<ImmutableSpi, Object> map = Keys.keyMap(prop, sources);
        Map<Object, Object> targetMap = (Map<Object, Object>) new BatchDataLoader(
                sqlClient,
                con,
                prop,
                filter
        ).load(map.values());
        for (Map.Entry<ImmutableSpi, Object> e : map.entrySet()) {
            e.setValue(targetMap.get(e.getValue()));
        }
        return (Map<S, T>) map;
    }
}
