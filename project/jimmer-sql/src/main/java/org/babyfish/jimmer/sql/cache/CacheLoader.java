package org.babyfish.jimmer.sql.cache;

import java.util.Map;
import java.util.Set;

public interface CacheLoader<K, V> {

    Map<K, V> loadAll(Set<K> keys, CacheFilter filter);
}
