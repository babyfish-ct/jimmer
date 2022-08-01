package org.babyfish.jimmer.sql.cache;

@FunctionalInterface
public interface CacheOperator {

    void delete(Cache<Object, Object> cache, Object key);
}
