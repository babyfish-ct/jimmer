package org.babyfish.jimmer.sql.ast.impl.util;

import org.jetbrains.annotations.Nullable;

public final class JdbcOptionValidator {

    private JdbcOptionValidator() {
    }

    public static void validateLocalFetchSize(@Nullable Integer fetchSize) {
        if (fetchSize != null && fetchSize < 0) {
            throw new IllegalArgumentException("`jdbcFetchSize` cannot be negative");
        }
    }

    public static void validateDefaultFetchSize(@Nullable Integer fetchSize) {
        if (fetchSize != null && fetchSize <= 0) {
            throw new IllegalArgumentException("`defaultJdbcFetchSize` must be greater than 0");
        }
    }

    public static void validateLocalQueryTimeout(@Nullable Integer queryTimeout) {
        if (queryTimeout != null && queryTimeout < 0) {
            throw new IllegalArgumentException("`jdbcQueryTimeout` cannot be negative");
        }
    }

    public static void validateDefaultQueryTimeout(@Nullable Integer queryTimeout) {
        if (queryTimeout != null && queryTimeout <= 0) {
            throw new IllegalArgumentException("`defaultJdbcQueryTimeout` must be greater than 0");
        }
    }
}
