package org.babyfish.jimmer.sql.fetcher;

public enum ReferenceFetchType {

    /**
     * Use `jimmer.default-reference-fetch-type`
     */
    AUTO,

    SELECT,

    /**
     * Limited by `jimmer.max-join-fetch-depth`
     */
    JOIN_IF_NO_CACHE,

    /**
     * Limited by `jimmer.max-join-fetch-depth`
     */
    JOIN_ALWAYS
}
