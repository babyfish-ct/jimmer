package org.babyfish.jimmer.sql.cache;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.SortedMap;

class ParameterizedUsedCacheImpl<K, V> extends UsedCacheImpl<K, V> implements Cache.Parameterized<K, V> {

    public ParameterizedUsedCacheImpl(Cache.Parameterized<K, V> raw, CacheOperator operator) {
        super(raw, operator);
    }

    @Override
    public @NotNull Map<K, V> getAll(
            @NotNull Collection<K> keys,
            @NotNull SortedMap<String, Object> parameterMap,
            @NotNull CacheEnvironment<K, V> env
    ) {
        return loading(() -> {
            Map<K, V> valueMap = ((Cache.Parameterized<K, V>)raw).getAll(keys, parameterMap, env);
            for (V value : valueMap.values()) {
                validateResult(value);
            }
            return valueMap;
        });
    }
}
