package org.babyfish.jimmer.sql.loader.impl;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.loader.spi.AbstractDataLoader;
import org.babyfish.jimmer.sql.fetcher.FieldFilter;
import org.babyfish.jimmer.sql.fetcher.impl.FetcherImpl;

import java.sql.Connection;

class DataLoader extends AbstractDataLoader {

    public DataLoader(
            JSqlClient sqlClient,
            Connection con,
            ImmutableProp prop,
            FieldFilter<?> filter
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
            JSqlClient sqlClient,
            Connection con,
            ImmutableProp prop,
            FieldFilter<?> filter,
            int limit,
            int offset
    ) {
        super(
                sqlClient,
                con,
                null,
                prop,
                prop.isAssociation(TargetLevel.PERSISTENT) ?
                        new FetcherImpl<>(prop.getTargetType().getJavaClass()).allTableFields() :
                        null,
                filter,
                limit,
                offset
        );
    }
}
