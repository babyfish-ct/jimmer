package org.babyfish.jimmer.sql.fetcher.impl;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.ast.query.Sortable;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.Field;
import org.babyfish.jimmer.sql.fetcher.Filter;
import org.babyfish.jimmer.sql.association.spi.AbstractBatchDataLoader;

import java.sql.Connection;
import java.util.*;

class BatchDataLoader extends AbstractBatchDataLoader {

    private final Field field;

    public BatchDataLoader(SqlClient sqlClient, Connection con, Field field) {
        super(sqlClient, con);
        this.field = field;
    }

    @Override
    protected ImmutableProp getProp() {
        return field.getProp();
    }

    @Override
    protected Fetcher<?> getChildFetcher() {
        return field.getChildFetcher();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void applyFilter(
            Sortable sortable,
            Table<ImmutableSpi> table,
            Collection<Object> keys
    ) {
        Filter<Table<ImmutableSpi>> filter =
                (Filter<Table<ImmutableSpi>>) field.getFilter();
        if (filter != null) {
            filter.apply(FilterArgsImpl.batchLoaderArgs(sortable, table, keys));
        }
    }
}
