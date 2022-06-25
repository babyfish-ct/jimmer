package org.babyfish.jimmer.sql.cache;

import java.util.Map;
import java.util.Set;

public interface CacheImplementation<V> extends CacheBinder {

    default boolean isNullSavable() {
        return true;
    }

    Map<String, V> getAll(Set<String> keys);

    void setAll(Map<String, V> map);

    void deleteAll(Set<String> keys);
}
