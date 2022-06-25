package org.babyfish.jimmer.sql.cache;

import java.util.Set;

public class CacheDeleteException extends RuntimeException {

    private final CacheImpl<?, ?> cache;

    private final Set<?> keys;

    CacheDeleteException(
            Throwable cause,
            CacheImpl<?, ?> cache,
            Set<?> keys
    ) {
        super("Failed to delete " + keys + "from cache" + cache, cause);
        this.cache = cache;
        this.keys = keys;
    }

    public CacheImpl<?, ?> getCache() {
        return cache;
    }

    public Set<?> getKeys() {
        return keys;
    }

    @SuppressWarnings("unchecked")
    public void retry() {
        Cache<Object, ?> cache = (Cache<Object, ?>) this.cache;
        Set<Object> keys = (Set<Object>) this.keys;
        cache.deleteAll(keys);
    }
}
