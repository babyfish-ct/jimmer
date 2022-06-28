package org.babyfish.jimmer.sql.cache.impl;

import org.babyfish.jimmer.sql.cache.CacheChain;
import org.babyfish.jimmer.sql.cache.CacheProvider;

import java.util.*;

public class CompositeProvider<V> implements CacheProvider<V> {

    private CacheProvider<V>[] providers;

    private CompositeProvider(CacheProvider<V>[] providers) {
        this.providers = providers;
    }

    @SuppressWarnings("unchecked")
    public static <V> CacheProvider<V> of(Collection<CacheProvider<V>> providers) {
        List<CacheProvider<V>> list = new ArrayList<>(providers.size());
        for (CacheProvider<V> provider : providers) {
            if (provider instanceof CompositeProvider<?>) {
                CompositeProvider<V> composite = (CompositeProvider<V>) provider;
                list.addAll(Arrays.asList(composite.providers));
            } else if (provider != null) {
                list.add(provider);
            }
        }
        if (list.isEmpty()) {
            throw new IllegalArgumentException("No provider is specified");
        }
        if (list.size() == 1) {
            return list.get(0);
        }
        return new CompositeProvider<>(
                list.toArray(
                        (CacheProvider<V>[]) new CacheProvider[list.size()]
                )
        );
    }

    @Override
    public Map<String, V> getAll(Set<String> storageKeys, CacheChain<V> chain) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteAll(Set<String> storageKeys) {
        throw new UnsupportedOperationException();
    }

    CacheProvider<V>[] providers() {
        return providers;
    }
}
