package org.babyfish.jimmer.sql.example.graphql.cache;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.babyfish.jimmer.sql.cache.chain.CacheChain;
import org.babyfish.jimmer.sql.cache.chain.LoadingBinder;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

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
                        new CacheLoader<K, Optional<V>>() {

                            @Override
                            public Optional<V> load(K key) {
                                Map<K, V> map = chain.loadAll(Collections.singleton(key));
                                V value = map.get(key);
                                if (value != null || map.containsKey(key)) {
                                    return Optional.ofNullable(value);
                                }
                                return Optional.ofNullable(null);
                            }

                            @SuppressWarnings("unchecked")
                            @Override
                            public Map<? extends K, ? extends Optional<V>> loadAll(Set<? extends K> keys) {
                                Map<K, V> map = chain.loadAll((Collection<K>) keys);
                                return map
                                        .entrySet()
                                        .stream()
                                        .collect(
                                                Collectors.toMap(
                                                        Map.Entry::getKey,
                                                        e -> Optional.ofNullable(e.getValue())
                                                )
                                        );
                            }
                        }
                );
    }

    @Override
    public Map<K, V> getAll(Collection<K> keys) {
        Map<K, Optional<V>> map = loadingCache.getAll(keys);
        Map<K, V> convertedMap = new HashMap<>((map.size() * 4 + 2) / 3);
        for (Map.Entry<K, Optional<V>> e : map.entrySet()) {
            convertedMap.put(e.getKey(), e.getValue().orElse(null));
        }
        return convertedMap;
    }

    @Override
    public void deleteAll(@NotNull Collection<K> keys, Object reason) {
        if (reason == null || reason.equals("caffeine")) {
            loadingCache.invalidateAll(keys);
        }
    }
}
