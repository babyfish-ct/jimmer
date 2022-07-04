package org.babyfish.jimmer.sql.cache;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

class CacheLoaderWrapper<K, V> implements CacheLoader<K, V> {

    private final CacheLoader<K, V> raw;

    CacheLoaderWrapper(CacheLoader<K, V> raw) {
        this.raw = raw;
    }

    static <K, V> CacheLoader<K, V> wrap(CacheLoader<K, V> loader) {
        if (loader instanceof CacheLoaderWrapper<?, ?>) {
            return loader;
        }
        return new CacheLoaderWrapper<>(loader);
    }

    @Override
    public Map<K, V> loadAll(Collection<K> keys) {
        if (keys.isEmpty()) {
            return Collections.emptyMap();
        }
        return raw.loadAll(keys);
    }
}
