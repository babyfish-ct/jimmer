package org.babyfish.jimmer.sql.cache;

@FunctionalInterface
public interface CacheOperator {

    void delete(LocatedCache<Object, ?> cache, Object key);
}
