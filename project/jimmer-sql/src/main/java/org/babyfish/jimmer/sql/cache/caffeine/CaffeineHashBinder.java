package org.babyfish.jimmer.sql.cache.caffeine;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.cache.CacheTracker;
import org.babyfish.jimmer.sql.cache.chain.SimpleBinder;
import org.babyfish.jimmer.sql.cache.spi.AbstractTrackingConsumerBinder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentMap;

public class CaffeineHashBinder<K, V> extends AbstractTrackingConsumerBinder<K> implements SimpleBinder.Parameterized<K, V> {

    private final Cache<K, Map<SortedMap<String, Object>, V>> cache;

    public CaffeineHashBinder(
            @Nullable ImmutableType type,
            @Nullable ImmutableProp prop,
            @Nullable CacheTracker tracker,
            int maximumSize,
            @NotNull Duration duration
    ) {
        super(type, prop, tracker);
        cache = Caffeine
                .newBuilder()
                .maximumSize(maximumSize)
                .expireAfterWrite(duration)
                .build();
    }

    @Override
    public Map<K, V> getAll(Collection<K> keys, SortedMap<String, Object> parameterMap) {
        Map<K, V> resultMap = new LinkedHashMap<>((keys.size() * 4 + 2) / 3);
        Map<K, Map<SortedMap<String, Object>, V>> subMapMap = cache.getAllPresent(keys);
        for (Map.Entry<K, Map<SortedMap<String, Object>, V>> e : subMapMap.entrySet()) {
            Map<SortedMap<String, Object>, V> subMap = e.getValue();
            V value = subMap.get(parameterMap);
            if (value != null || subMap.containsKey(parameterMap)) {
                resultMap.put(e.getKey(), value);
            }
        }
        return resultMap;
    }

    @Override
    public void setAll(Map<K, V> map, SortedMap<String, Object> parameterMap) {
        ConcurrentMap<K, Map<SortedMap<String, Object>, V>> cacheMap = cache.asMap();
        for (Map.Entry<K, V> e : map.entrySet()) {
            cacheMap.compute(e.getKey(), (key, oldSubMap) -> {
                Map<SortedMap<String, Object>, V> subMap =
                        oldSubMap != null ? new HashMap<>(oldSubMap) : new HashMap<>();
                subMap.put(parameterMap, e.getValue());
                return subMap;
            });
        }
    }

    @Override
    public void deleteAllImpl(@NotNull Collection<K> keys) {
        cache.invalidateAll(keys);
    }

    @Override
    protected void invalidateAll() {
        cache.invalidateAll();
    }

    @Override
    protected boolean matched(@Nullable Object reason) {
        return "caffeine".equals(reason);
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

        public CaffeineHashBinder<K, V> build() {
            return new CaffeineHashBinder<>(
                    type,
                    prop,
                    tracker,
                    maximumSize,
                    duration
            );
        }
    }
}
