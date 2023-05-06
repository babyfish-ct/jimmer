package org.babyfish.jimmer.sql.fetcher.impl;

import org.babyfish.jimmer.sql.loader.AbstractDataLoader;
import org.babyfish.jimmer.sql.fetcher.Field;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;

import java.sql.Connection;

public class DataLoader extends AbstractDataLoader {

    public DataLoader(JSqlClientImplementor sqlClient, Connection con, Field field) {
        super(
                sqlClient,
                con,
                field.getEntityType(),
                field.getProp(),
                field.getChildFetcher(),
                field.getFilter(),
                field.getLimit(),
                field.getOffset()
        );
    }
}
