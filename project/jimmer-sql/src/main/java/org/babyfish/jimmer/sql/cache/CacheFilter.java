package org.babyfish.jimmer.sql.cache;

import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.event.AssociationEvent;
import org.babyfish.jimmer.sql.event.EntityEvent;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public interface CacheFilter {

    /**
     * Get arguments of this filter
     * @return A map, never null.
     */
    @NotNull
    default NavigableMap<String, Object> getArgs() {
        return Collections.emptyNavigableMap();
    }

    default boolean isAffectedBy(EntityEvent<?> e) {
        return false;
    }

    default boolean isAffectedBy(AssociationEvent e) {
        return false;
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
        return cacheFilter == null || cacheFilter.getArgs().isEmpty();
    }
}