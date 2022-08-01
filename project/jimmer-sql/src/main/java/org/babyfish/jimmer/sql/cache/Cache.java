package org.babyfish.jimmer.sql.cache;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public interface Cache<K, V> {
    
    default V get(K key, CacheEnvironment<K, V> env) {
        Map<K, V> map = getAll(Collections.singleton(key), env);
        return map.get(key);
    }

    Map<K, V> getAll(Collection<K> keys, CacheEnvironment<K, V> env);

    default void delete(K key) {
        deleteAll(Collections.singleton(key));
    }

    void deleteAll(Collection<K> keys);
}
