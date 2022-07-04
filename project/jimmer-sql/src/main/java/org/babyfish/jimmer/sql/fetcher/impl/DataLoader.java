package org.babyfish.jimmer.sql.fetcher.impl;

import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.association.spi.AbstractDataLoader;
import org.babyfish.jimmer.sql.fetcher.Field;

import java.sql.Connection;

public class DataLoader extends AbstractDataLoader {

    public DataLoader(SqlClient sqlClient, Connection con, Field field) {
        super(sqlClient, con, field.getProp(), field.getChildFetcher(), field.getFilter());
    }
}
