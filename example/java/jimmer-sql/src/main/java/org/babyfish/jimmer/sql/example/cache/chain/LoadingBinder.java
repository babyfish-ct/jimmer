package org.babyfish.jimmer.sql.example.cache.chain;

import java.util.Collection;
import java.util.Map;

public interface LoadingBinder<K, V> {

    void initialize(CacheChain<K, V> chain);

    Map<K, V> getAll(Collection<K> keys);

    void deleteAll(Collection<K> keys);
}
