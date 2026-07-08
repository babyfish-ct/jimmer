package org.babyfish.jimmer.sql.runtime;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class JdbcOptions {

    public static final JdbcOptions EMPTY = new JdbcOptions(null, null);

    @Nullable
    private final Integer fetchSize;

    @Nullable
    private final Integer queryTimeout;

    private JdbcOptions(@Nullable Integer fetchSize, @Nullable Integer queryTimeout) {
        this.fetchSize = fetchSize;
        this.queryTimeout = queryTimeout;
    }

    public static JdbcOptions of(@Nullable Integer fetchSize, @Nullable Integer queryTimeout) {
        if (fetchSize == null && queryTimeout == null) {
            return EMPTY;
        }
        return new JdbcOptions(fetchSize, queryTimeout);
    }

    @Nullable
    public Integer getFetchSize() {
        return fetchSize;
    }

    @Nullable
    public Integer getQueryTimeout() {
        return queryTimeout;
    }

    public JdbcOptions fetchSize(@Nullable Integer fetchSize) {
        return of(fetchSize, queryTimeout);
    }

    public JdbcOptions queryTimeout(@Nullable Integer queryTimeout) {
        return of(fetchSize, queryTimeout);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof JdbcOptions)) {
            return false;
        }
        JdbcOptions that = (JdbcOptions) o;
        return Objects.equals(fetchSize, that.fetchSize) &&
                Objects.equals(queryTimeout, that.queryTimeout);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fetchSize, queryTimeout);
    }
}
