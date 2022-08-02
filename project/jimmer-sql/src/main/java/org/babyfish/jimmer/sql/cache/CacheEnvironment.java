package org.babyfish.jimmer.sql.cache;

import org.babyfish.jimmer.sql.JSqlClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.util.Objects;

public class CacheEnvironment<K, V> {

    private final JSqlClient sqlClient;

    private final Connection connection;

    private final CacheFilter filter;

    private final CacheLoader<K, V> loader;

    public CacheEnvironment(
            JSqlClient sqlClient,
            Connection connection,
            CacheFilter filter,
            CacheLoader<K, V> loader,
            boolean requiresNewDraftContext) {
        this.sqlClient = Objects.requireNonNull(sqlClient, "sqlClient cannot be null");
        this.connection = Objects.requireNonNull(connection, "connection cannot be null");
        this.filter = filter;
        this.loader = CacheLoaderWrapper.wrap(
                Objects.requireNonNull(loader, "loader cannot be null"),
                requiresNewDraftContext
        );
    }

    @NotNull
    public JSqlClient getSqlClient() {
        return sqlClient;
    }

    @NotNull
    public Connection getConnection() {
        return connection;
    }

    @Nullable
    public CacheFilter getFilter() {
        return filter;
    }

    @NotNull
    public CacheLoader<K, V> getLoader() {
        return loader;
    }

    @Override
    public int hashCode() {
        return Objects.hash(sqlClient, connection, filter, loader);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CacheEnvironment<?, ?> that = (CacheEnvironment<?, ?>) o;
        return sqlClient.equals(that.sqlClient) &&
                connection.equals(that.connection) &&
                Objects.equals(filter, that.filter) &&
                Objects.equals(loader, that.loader);
    }

    @Override
    public String toString() {
        return "CacheEnvironment{" +
                "sqlClient=" + sqlClient +
                ", connection=" + connection +
                ", filter=" + filter +
                ", loader=" + loader +
                '}';
    }
}

