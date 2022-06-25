package org.babyfish.jimmer.sql.association.loader;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.association.spi.AbstractBatchDataLoader;
import org.babyfish.jimmer.sql.ast.query.Sortable;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.fetcher.Filter;
import org.babyfish.jimmer.sql.fetcher.impl.FilterArgsImpl;

import java.sql.Connection;
import java.util.Collection;

class BatchDataLoader extends AbstractBatchDataLoader {

    private ImmutableProp prop;

    private Filter<Table<ImmutableSpi>> filter;

    public BatchDataLoader(
            SqlClient sqlClient,
            Connection con,
            ImmutableProp prop,
            Filter<Table<ImmutableSpi>> filter
    ) {
        super(sqlClient, con);
        this.prop = prop;
        this.filter = filter;
    }

    @Override
    protected ImmutableProp getProp() {
        return prop;
    }

    @Override
    protected void applyFilter(Sortable sortable, Table<ImmutableSpi> table, Collection<Object> keys) {
        if (filter != null) {
            filter.apply(FilterArgsImpl.batchLoaderArgs(sortable, table, keys));
        }
    }
}
