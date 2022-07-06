package org.babyfish.jimmer.sql.association.loader;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.association.spi.AbstractDataLoader;
import org.babyfish.jimmer.sql.fetcher.Filter;
import org.babyfish.jimmer.sql.fetcher.impl.FetcherImpl;

import java.sql.Connection;

class DataLoader extends AbstractDataLoader {

    public DataLoader(
            SqlClient sqlClient,
            Connection con,
            ImmutableProp prop,
            Filter<?> filter
    ) {
        this(
                sqlClient,
                con,
                prop,
                filter,
                Integer.MAX_VALUE,
                0);
    }

    public DataLoader(
            SqlClient sqlClient,
            Connection con,
            ImmutableProp prop,
            Filter<?> filter,
            int limit,
            int offset
    ) {
        super(
                sqlClient,
                con,
                prop,
                new FetcherImpl<>(prop.getTargetType().getJavaClass()).allTableFields(),
                filter,
                limit,
                offset
        );
    }
}
