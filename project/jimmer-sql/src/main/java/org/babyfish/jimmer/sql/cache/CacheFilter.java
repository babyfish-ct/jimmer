package org.babyfish.jimmer.sql.cache;

import org.babyfish.jimmer.sql.ast.tuple.Tuple2;

import java.util.*;

public interface CacheFilter {

    /**
     * Get arguments of this filter
     * @return A map, never null.
     */
    default NavigableMap<String, Object> toCacheArgs() {
        return Collections.emptyNavigableMap();
    }

    static CacheFilter of(Map<String, Object> args) {
        return new CacheFilterImpl(args);
    }

    static CacheFilter of(List<Tuple2<String, Object>> tuples) {
        return new CacheFilterImpl(tuples);
    }

    @SafeVarargs
    static CacheFilter of(Tuple2<String, Object> ... tuples) {
        return new CacheFilterImpl(Arrays.asList(tuples));
    }

    static boolean isEmpty(CacheFilter cacheFilter) {
        return cacheFilter == null || cacheFilter.toCacheArgs().isEmpty();
    }
}