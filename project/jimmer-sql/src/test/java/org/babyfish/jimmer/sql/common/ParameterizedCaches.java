package org.babyfish.jimmer.sql.common;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.cache.Cache;
import org.babyfish.jimmer.sql.cache.chain.CacheChain;
import org.babyfish.jimmer.sql.cache.chain.ChainCacheBuilder;
import org.babyfish.jimmer.sql.cache.chain.LoadingBinder;
import org.babyfish.jimmer.sql.cache.chain.SimpleBinder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ParameterizedCaches {

    private ParameterizedCaches() {}

    public static <K, V> Cache<K, V> create() {
        return create(null, null);
    }
    
    public static <K, V> Cache<K, V> create(ImmutableProp prop, BiConsumer<ImmutableProp, Collection<K>> onDelete) {
        return new ChainCacheBuilder<K, V>()
                .add(new LevelOneBinder<>(prop, onDelete))
                .add(new LevelTwoBinder<>())
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

        private final ImmutableProp prop;

        private final BiConsumer<ImmutableProp, Collection<K>> onDelete;
        
        private final Map<K, Map<Map<String, Object>, V>> valueMap = new HashMap<>();

        private CacheChain.Parameterized<K, V> chain;

        LevelOneBinder(ImmutableProp prop, BiConsumer<ImmutableProp, Collection<K>> onDelete) {
            this.prop = prop;
            this.onDelete = onDelete;
        }

        @Override
        public void initialize(@NotNull CacheChain.Parameterized<K, V> chain) {
            this.chain = chain;
        }

        @Override
        public @NotNull Map<K, V> getAll(
                @NotNull Collection<K> keys,
                @NotNull NavigableMap<String, Object> parameterMap
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
            if (onDelete != null) {
                onDelete.accept(prop, keys);
            }
        }
    }

    private static class LevelTwoBinder<K, V> implements SimpleBinder.Parameterized<K, V> {
        
        private final Map<K, Map<Map<String, Object>, V>> valueMap = new HashMap<>();
        
        @Override
        public @NotNull Map<K, V> getAll(@NotNull Collection<K> keys, @NotNull NavigableMap<String, Object> parameterMap) {
            return read(valueMap, keys, parameterMap);
        }

        @Override
        public void setAll(@NotNull Map<K, V> map, @NotNull NavigableMap<String, Object> parameterMap) {
            write(valueMap, map, parameterMap);
        }

        @Override
        public void deleteAll(@NotNull Collection<K> keys, @Nullable Object reason) {
            valueMap.keySet().removeAll(keys);
        }
    }
}
