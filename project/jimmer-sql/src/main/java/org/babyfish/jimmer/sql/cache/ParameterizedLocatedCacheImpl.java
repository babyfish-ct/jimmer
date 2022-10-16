package org.babyfish.jimmer.sql.cache;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.SortedMap;

class ParameterizedLocatedCacheImpl<K, V> extends LocatedCacheImpl<K, V> implements Cache.Parameterized<K, V> {

    public ParameterizedLocatedCacheImpl(Cache.Parameterized<K, V> raw, ImmutableType type, ImmutableProp prop) {
        super(raw, type, prop);
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
