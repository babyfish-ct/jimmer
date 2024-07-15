package org.babyfish.jimmer.sql.cache.caffeine;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.babyfish.jimmer.lang.Ref;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.cache.CacheTracker;
import org.babyfish.jimmer.sql.cache.chain.CacheChain;
import org.babyfish.jimmer.sql.cache.chain.LoadingBinder;
import org.babyfish.jimmer.sql.cache.spi.AbstractBinder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public class CaffeineValueBinder<K, V> extends AbstractBinder<K> implements LoadingBinder<K, V> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaffeineValueBinder.class);

    private final int maximumSize;

    private final Duration duration;

    // Caffeine does not support null value, use `Ref` as a wrapper
    private LoadingCache<K, Ref<V>> loadingCache;

    protected CaffeineValueBinder(
            @Nullable ImmutableType type,
            @Nullable ImmutableProp prop,
            @Nullable CacheTracker tracker,
            int maximumSize,
            @NotNull Duration duration
    ) {
        super(type, prop, tracker);
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
    public void deleteAll(Collection<K> keys, Object reason) {
        if (reason == null || reason.equals("caffeine")) {
            loadingCache.invalidateAll(keys);
        }
    }

    @Override
    protected void invalidateAll() {
        loadingCache.invalidateAll();
    }

    @NotNull
    public static <K, V> Builder<K, V> forObject(ImmutableType type) {
        return new Builder<>(type, null);
    }

    @NotNull
    public static <K, V> Builder<K, V> forProp(ImmutableProp prop) {
        return new Builder<>(null, prop);
    }

    public static class Builder<K, V> {
        private final ImmutableType type;
        private final ImmutableProp prop;
        private CacheTracker tracker;
        private int maximumSize = 100;
        private Duration duration = Duration.ofMinutes(1);

        public Builder(ImmutableType type, ImmutableProp prop) {
            this.type = type;
            this.prop = prop;
        }

        public Builder<K, V> subscribe(CacheTracker tracker) {
            this.tracker = tracker;
            return this;
        }

        public Builder<K, V> maximumSize(int maximumSize) {
            this.maximumSize = maximumSize;
            return this;
        }

        public Builder<K, V> duration(Duration duration) {
            this.duration = duration;
            return this;
        }

        public CaffeineValueBinder<K, V> build() {
            return new CaffeineValueBinder<>(
                    type,
                    prop,
                    tracker,
                    maximumSize,
                    duration
            );
        }
    }
}
