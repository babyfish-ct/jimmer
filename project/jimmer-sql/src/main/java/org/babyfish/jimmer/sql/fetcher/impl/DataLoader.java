package org.babyfish.jimmer.sql.fetcher.impl;

import org.babyfish.jimmer.sql.loader.AbstractDataLoader;
import org.babyfish.jimmer.sql.fetcher.Field;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;

import java.sql.Connection;

public class DataLoader extends AbstractDataLoader {

    public DataLoader(JSqlClientImplementor sqlClient, Connection con, FetchPath path, Field field) {
        super(
                sqlClient,
                con,
                field.getEntityType(),
                path,
                field.getProp(),
                field.getChildFetcher(true),
                field.getRecursionStrategy(),
                field.getFilter(),
                field.getLimit(),
                field.getOffset(),
                field.isRawId()
        );
    }
}
