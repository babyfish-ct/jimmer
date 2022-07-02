package org.babyfish.jimmer.sql.cache;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public interface Cache<K, V> {
    
    default V get(K key, CacheEnvironment env) {
        Map<K, V> map = getAll(Collections.singleton(key), env);
        return map.get(key);
    }

    Map<K, V> getAll(Collection<K> keys, CacheEnvironment env);

    default void delete(K key, CacheEnvironment env) {
        deleteAll(Collections.singleton(key), env);
    }

    void deleteAll(Collection<K> keys, CacheEnvironment env);
}
