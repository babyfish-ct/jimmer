package org.babyfish.jimmer.sql.association.loader;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.association.spi.AbstractSingleDataLoader;
import org.babyfish.jimmer.sql.ast.query.Sortable;
import org.babyfish.jimmer.sql.ast.table.Table;

import java.sql.Connection;
import java.util.function.BiConsumer;

class SingleDataLoader extends AbstractSingleDataLoader {

    private ImmutableProp prop;

    private BiConsumer<Sortable, Table<?>> filter;

    private int limit;

    private int offset;

    public SingleDataLoader(
            SqlClient sqlClient,
            Connection con,
            ImmutableProp prop,
            BiConsumer<Sortable, Table<?>> filter,
            int limit,
            int offset
    ) {
        super(sqlClient, con);
        this.prop = prop;
        this.filter = filter;
        this.limit = limit;
        this.offset = offset;
    }

    @Override
    protected ImmutableProp getProp() {
        return prop;
    }

    @Override
    protected void applyFilter(Sortable sortable, Table<ImmutableSpi> table, Object key) {
        if (filter != null) {
            filter.accept(sortable, table);
        }
    }

    @Override
    protected int getLimit() {
        return limit;
    }

    @Override
    protected int getOffset() {
        return offset;
    }
}
