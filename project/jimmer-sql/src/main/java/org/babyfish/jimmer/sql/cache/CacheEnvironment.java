package org.babyfish.jimmer.sql.cache;

import org.babyfish.jimmer.sql.JSqlClient;

import java.sql.Connection;
import java.util.Objects;

public class CacheEnvironment {

    private final JSqlClient sqlClient;

    private final Connection connection;

    private final CacheFilter filter;

    public CacheEnvironment(JSqlClient sqlClient, Connection connection) {
        this(sqlClient, connection, null);
    }

    public CacheEnvironment(JSqlClient sqlClient, Connection connection, CacheFilter filter) {
        this.sqlClient = Objects.requireNonNull(sqlClient, "sqlClient cannot be null");
        this.connection = Objects.requireNonNull(connection, "connection cannot be null");
        this.filter = filter;
    }

    public JSqlClient getSqlClient() {
        return sqlClient;
    }

    public Connection getConnection() {
        return connection;
    }

    public CacheFilter getFilter() {
        return filter;
    }

    @Override
    public int hashCode() {
        return Objects.hash(sqlClient, connection, filter);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CacheEnvironment that = (CacheEnvironment) o;
        return sqlClient.equals(that.sqlClient) &&
                connection.equals(that.connection) &&
                Objects.equals(filter, that.filter);
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
