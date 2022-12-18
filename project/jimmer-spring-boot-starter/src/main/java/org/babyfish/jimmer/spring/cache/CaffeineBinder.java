package org.babyfish.jimmer.spring.cache;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.babyfish.jimmer.lang.Ref;
import org.babyfish.jimmer.sql.cache.chain.CacheChain;
import org.babyfish.jimmer.sql.cache.chain.LoadingBinder;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public class CaffeineBinder<K, V> implements LoadingBinder<K, V> {

    private final int maximumSize;

    private final Duration duration;

    // Caffeine does not support null value, use `Ref` as a wrapper
    private LoadingCache<K, Ref<V>> loadingCache;

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
                        new CacheLoader<K, Ref<V>>() {

                            @Override
                            public Ref<V> load(K key) {
                                Map<K, V> map = chain.loadAll(Collections.singleton(key));
                                V value = map.get(key);
                                if (value != null || map.containsKey(key)) {
                                    return Ref.of(value);
                                }
                                return null;
                            }

                            @SuppressWarnings("unchecked")
                            @Override
                            public Map<K, Ref<V>> loadAll(Iterable<? extends K> keys) {
                                Map<K, V> map = chain.loadAll((Collection<K>) keys);
                                return map
                                        .entrySet()
                                        .stream()
                                        .collect(
                                                Collectors.toMap(
                                                        Map.Entry::getKey,
                                                        e -> Ref.of(e.getValue())
                                                )
                                        );
                            }
                        }
                );
    }

    @Override
    public Map<K, V> getAll(Collection<K> keys) {
        Map<K, Ref<V>> map = loadingCache.getAll(keys);
        Map<K, V> convertedMap = new HashMap<>((map.size() * 4 + 2) / 3);
        for (Map.Entry<K, Ref<V>> e : map.entrySet()) {
            convertedMap.put(e.getKey(), e.getValue().getValue());
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
