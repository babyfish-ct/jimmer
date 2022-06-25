package org.babyfish.jimmer.sql.cache;

import java.util.Collection;
import java.util.Map;

public interface Cache<K, V> {

    default V get(K key) {
        return get(key, null);
    }

    V get(K key, CacheFilter filter);

    default Map<K, V> getAll(Collection<K> keys) {
        return getAll(keys, null);
    }

    Map<K, V> getAll(Collection<K> keys, CacheFilter filter);

    default void delete(K key) {
        delete(key, null);
    }

    void delete(K key, CacheFilter filter);

    default void deleteAll(Collection<K> keys) {
        deleteAll(keys, null);
    }

    void deleteAll(Collection<K> keys, CacheFilter filter);
}
