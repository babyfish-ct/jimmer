package org.babyfish.jimmer.sql.cache;

import org.babyfish.jimmer.sql.SqlClient;

import java.sql.Connection;
import java.util.Objects;

public class QueryCacheEnvironment<K, V> extends CacheEnvironment {

    private CacheLoader<K, V> loader;

    public QueryCacheEnvironment(
            SqlClient sqlClient,
            Connection connection,
            CacheFilter filter,
            CacheLoader<K, V> loader
    ) {
        super(sqlClient, connection, filter);
        this.loader = Objects.requireNonNull(loader, "loader cannot be null");
    }

    public CacheLoader<K, V> getLoader() {
        return loader;
    }
}
