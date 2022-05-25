package org.babyfish.jimmer.sql.fetcher.impl;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.association.meta.AssociationType;
import org.babyfish.jimmer.sql.association.spi.AbstractSingleDataLoader;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.impl.query.Queries;
import org.babyfish.jimmer.sql.ast.query.Sortable;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.Field;
import org.babyfish.jimmer.sql.fetcher.Filter;
import org.babyfish.jimmer.sql.meta.Column;

import java.sql.Connection;
import java.util.*;

class SingleDataLoader extends AbstractSingleDataLoader {

    private final Field field;

    public SingleDataLoader(SqlClient sqlClient, Connection con, Field field) {
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
            Object key
    ) {
        Filter<ImmutableSpi, Table<ImmutableSpi>> filter =
                (Filter<ImmutableSpi, Table<ImmutableSpi>>) field.getFilter();
        if (filter != null) {
            filter.apply(FilterArgsImpl.singleLoaderArgs(sortable, table, key));
        }
    }

    @Override
    protected int getLimit() {
        return field.getLimit();
    }

    @Override
    protected int getOffset() {
        return field.getOffset();
    }
}
