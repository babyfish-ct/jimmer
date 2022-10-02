package org.babyfish.jimmer.sql.fetcher.impl;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.loader.spi.AbstractDataLoader;
import org.babyfish.jimmer.sql.fetcher.Field;

import java.sql.Connection;

public class DataLoader extends AbstractDataLoader {

    public DataLoader(JSqlClient sqlClient, Connection con, Field field) {
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
