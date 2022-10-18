package org.babyfish.jimmer.sql.cache.chain;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.SortedMap;

public interface SimpleBinder<K, V> {

    Map<K, V> getAll(Collection<K> keys);

    void setAll(Map<K, V> map);

    void deleteAll(Collection<K> keys, Object reason);

    interface Parameterized<K, V> extends SimpleBinder<K, V> {

        default Map<K, V> getAll(Collection<K> keys) {
            return getAll(keys, Collections.emptySortedMap());
        }

        default void setAll(Map<K, V> map) {
            setAll(map, Collections.emptySortedMap());
        }

        Map<K, V> getAll(
                Collection<K> keys,
                SortedMap<String, Object> parameterMap
        );

        void setAll(
                Map<K, V> map,
                SortedMap<String, Object> parameterMap
        );
    }
}
