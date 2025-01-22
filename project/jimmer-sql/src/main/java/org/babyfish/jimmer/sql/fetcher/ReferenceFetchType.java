package org.babyfish.jimmer.sql.fetcher;

public enum ReferenceFetchType {

    /**
     * Use `jimmer.default-reference-fetch-type`
     */
    AUTO,

    SELECT,

    /**
     * Limmited by `jimmer.max-join-fetch-depth`
     */
    JOIN_IF_NO_CACHE,

    /**
     * Limmited by `jimmer.max-join-fetch-depth`
     */
    JOIN_ALWAYS
}
