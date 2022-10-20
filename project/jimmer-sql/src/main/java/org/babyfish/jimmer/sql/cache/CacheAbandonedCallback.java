package org.babyfish.jimmer.sql.cache;

import org.babyfish.jimmer.meta.ImmutableProp;

public interface CacheAbandonedCallback {

    void abandoned(ImmutableProp prop, Reason reason);

    enum Reason {

        /**
         * Associated objects are filtered by some filters,
         * but some filters are not cacheable filter.
         */
        CACHEABLE_FILTER_REQUIRED,

        /**
         * The method `getParameters()` of
         * cacheable filter or parameterized transient resolver
         * returns a map which is not empty,
         * but the cache is not parameterized.
         */
        PARAMETERIZED_CACHE_REQUIRED,

        /**
         * The field level filter of object fetcher or graphql is used
         */
        FIELD_FILTER_USED
    }
}
