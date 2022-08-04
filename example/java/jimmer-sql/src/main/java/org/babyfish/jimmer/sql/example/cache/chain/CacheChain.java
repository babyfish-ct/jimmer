package org.babyfish.jimmer.sql.example.cache.chain;

import java.util.Collection;
import java.util.Map;

public interface CacheChain<K, V> {
    Map<K, V> loadAll(Collection<K> keys);
}