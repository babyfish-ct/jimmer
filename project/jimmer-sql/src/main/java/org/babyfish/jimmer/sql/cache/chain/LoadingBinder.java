package org.babyfish.jimmer.sql.cache.chain;

import java.util.*;

public interface LoadingBinder<K, V> {

    void initialize(CacheChain<K, V> chain);

    Map<K, V> getAll(Collection<K> keys);

    void deleteAll(Collection<K> keys, Object reason);

    interface Parameterized<K, V> {

        void initialize(CacheChain.Parameterized<K, V> chain);

        default Map<K, V> getAll(Collection<K> keys) {
            return getAll(keys, Collections.emptySortedMap());
        }

        Map<K, V> getAll(
                Collection<K> keys,
                SortedMap<String, Object> parameterMap
        );

        void deleteAll(Collection<K> keys, Object reason);
    }
}
