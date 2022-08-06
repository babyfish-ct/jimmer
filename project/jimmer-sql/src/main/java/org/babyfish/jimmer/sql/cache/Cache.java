package org.babyfish.jimmer.sql.cache;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public interface Cache<K, V> {
    
    default Optional<V> get(K key, CacheEnvironment<K, V> env) {
        Map<K, V> map = getAll(Collections.singleton(key), env);
        V value = map.get(key);
        if (value == null && map.containsKey(key)) {
            return null;
        }
        return Optional.ofNullable(value);
    }

    Map<K, V> getAll(Collection<K> keys, CacheEnvironment<K, V> env);

    default void delete(K key) {
        deleteAll(Collections.singleton(key));
    }

    void deleteAll(Collection<K> keys);
}
