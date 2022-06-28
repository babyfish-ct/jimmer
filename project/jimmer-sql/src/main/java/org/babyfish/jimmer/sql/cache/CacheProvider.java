package org.babyfish.jimmer.sql.cache;

import org.babyfish.jimmer.sql.cache.impl.CompositeProvider;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface CacheProvider<V> {

    Map<String, V> getAll(Set<String> storageKeys, CacheChain<V> chain);

    void deleteAll(Set<String> storageKeys);

    static <V> CacheProvider<V> of(Collection<CacheProvider<V>> providers) {
        return CompositeProvider.of(providers);
    }

    static <V> CacheProvider<V> of(CacheProvider<V> ... providers) {
        return CompositeProvider.of(Arrays.asList(providers));
    }
}
