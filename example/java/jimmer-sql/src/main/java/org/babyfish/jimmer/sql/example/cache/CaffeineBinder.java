package org.babyfish.jimmer.sql.example.cache;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.babyfish.jimmer.sql.example.cache.chain.CacheChain;
import org.babyfish.jimmer.sql.example.cache.chain.LoadingBinder;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.time.Duration;
import java.util.*;

// Level-1 Cache
public class CaffeineBinder<K, V> implements LoadingBinder<K, V> {

    private final int maximumSize;

    private final Duration duration;

    // Caffeine does not support null value, use optional as a wrapper
    private LoadingCache<K, Optional<V>> loadingCache;

    public CaffeineBinder(int maximumSize, Duration duration) {
        this.maximumSize = maximumSize;
        this.duration = duration;
    }

    @Override
    public void initialize(CacheChain<K, V> chain) {
        loadingCache = Caffeine
                .newBuilder()
                .maximumSize(maximumSize)
                .expireAfterWrite(duration)
                .build(
                        new CacheLoader<K, V>() {

                            @Override
                            public Optional<V> load(K key) throws Exception {
                                return chain.loadAll(Collections.singleton(key)).get(key);
                            }

                            @SuppressWarnings("unchecked")
                            @Override
                            public Map<? extends K, ? extends V> loadAll(Set<? extends K> keys) throws Exception {
                                return chain.loadAll((Set<K>)keys);
                            }
                        }
                );
    }

    @Override
    public Map<K, V> getAll(Collection<K> keys) {
        return loadingCache.getAll(keys);
    }

    @Override
    public void deleteAll(Collection<K> keys) {
        loadingCache.invalidateAll(keys);
    }
}
