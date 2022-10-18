package org.babyfish.jimmer.sql.common;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.cache.Cache;
import org.babyfish.jimmer.sql.cache.chain.CacheChain;
import org.babyfish.jimmer.sql.cache.chain.ChainCacheBuilder;
import org.babyfish.jimmer.sql.cache.chain.LoadingBinder;
import org.babyfish.jimmer.sql.cache.chain.SimpleBinder;
import org.babyfish.jimmer.sql.cache.spi.AbstractRemoteHashBinder;
import org.babyfish.jimmer.sql.model.BookStoreProps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ParameterizedCaches {

    private ParameterizedCaches() {}

    public static <K, V> Cache<K, V> create(ImmutableProp prop) {
        return create(prop, null);
    }
    
    public static <K, V> Cache<K, V> create(ImmutableProp prop, Consumer<Collection<String>> onDelete) {
        return new ChainCacheBuilder<K, V>()
                .add(new LevelOneBinder<>())
                .add(new LevelTwoBinder<>(prop, onDelete))
                .build();
    }

    private static <K, V> Map<K, V> read(
            Map<K, Map<Map<String, Object>, V>> valueMap,
            Collection<K> keys,
            Map<String, Object> parameterMap
    ) {
        Map<K, V> map = new HashMap<>();
        for (K key : keys) {
            Map<Map<String, Object>, V> subMap = valueMap.get(key);
            if (subMap != null) {
                V value = subMap.get(parameterMap);
                if (value != null || subMap.containsKey(parameterMap)) {
                    map.put(key, value);
                }
            }
        }
        return map;
    }

    private static <K, V> Map<K, V> write(
            Map<K, Map<Map<String, Object>, V>> valueMap,
            Map<K, V> map,
            Map<String, Object> parameterMap
    ) {
        for (Map.Entry<K, V> entry : map.entrySet()) {
            Map<Map<String, Object>, V> subMap = valueMap.computeIfAbsent(entry.getKey(), it -> new HashMap<>());
            subMap.put(parameterMap, entry.getValue());
        }
        return map;
    }

    private static class LevelOneBinder<K, V> implements LoadingBinder.Parameterized<K, V> {
        
        private final Map<K, Map<Map<String, Object>, V>> valueMap = new HashMap<>();

        private CacheChain.Parameterized<K, V> chain;

        @Override
        public void initialize(@NotNull CacheChain.Parameterized<K, V> chain) {
            this.chain = chain;
        }

        @Override
        public @NotNull Map<K, V> getAll(
                @NotNull Collection<K> keys,
                @NotNull SortedMap<String, Object> parameterMap
        ) {
            Map<K, V> map = read(valueMap, keys, parameterMap);
            if (map.size() < keys.size()) {
                Set<K> missedKeys = new LinkedHashSet<>();
                for (K key : keys) {
                    if (!map.containsKey(key)) {
                        missedKeys.add(key);
                    }
                }
                Map<K, V> mapFromNext = chain.loadAll(keys, parameterMap);
                if (mapFromNext.size() < missedKeys.size()) {
                    for (K missedKey : missedKeys) {
                        if (!mapFromNext.containsKey(missedKey)) {
                            mapFromNext.put(missedKey, null);
                        }
                    }
                }
                write(valueMap, mapFromNext, parameterMap);
                map.putAll(mapFromNext);
            }
            return map;
        }

        @Override
        public void deleteAll(@NotNull Collection<K> keys, @Nullable Object reason) {
            valueMap.keySet().removeAll(keys);
        }
    }

    private static class LevelTwoBinder<K, V> extends AbstractRemoteHashBinder<K, V> {

        private final Map<String, Map<String, byte[]>> valueMap = new HashMap<>();


        private final Consumer<Collection<String>> onDelete;

        protected LevelTwoBinder(ImmutableProp prop, Consumer<Collection<String>> onDelete) {
            super(null, null, prop, Duration.ofSeconds(10), 0);
            this.onDelete = onDelete;
        }

        @Override
        protected void delete(Collection<String> keys) {
            valueMap.keySet().removeAll(keys);
            if (onDelete != null) {
                onDelete.accept(keys);
            }
            valueMap.keySet().removeAll(keys);
        }

        @Override
        protected List<byte[]> read(Collection<String> keys, String hashKey) {
            List<byte[]> arr = new ArrayList<>();
            for (String key : keys) {
                Map<String, byte[]> subMap = valueMap.get(key);
                if (subMap != null) {
                    arr.add(subMap.get(hashKey));
                } else {
                    arr.add(null);
                }
            }
            return arr;
        }

        @Override
        protected void write(Map<String, byte[]> map, String hashKey) {
            for (Map.Entry<String, byte[]> e : map.entrySet()) {
                Map<String, byte[]> subMap = valueMap.computeIfAbsent(e.getKey(), it -> new HashMap<>());
                subMap.put(hashKey, e.getValue());
            }
        }

        @Override
        protected String reason() {
            return null;
        }
    }
}
