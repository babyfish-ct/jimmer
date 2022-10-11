package org.babyfish.jimmer.sql.common;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.cache.Cache;
import org.babyfish.jimmer.sql.cache.CacheEnvironment;
import org.babyfish.jimmer.sql.cache.ValueSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class CacheImpl<T> implements Cache<Object, T> {

    private final Map<Object, byte[]> map = new HashMap<>();

    private final ValueSerializer<T> valueSerializer;

    public CacheImpl(ImmutableType type) {
        valueSerializer = new ValueSerializer<>(type);
    }

    public CacheImpl(ImmutableProp prop) {
        valueSerializer = new ValueSerializer<>(prop);
    }

    @NotNull
    @Override
    public Map<Object, T> getAll(@NotNull Collection<Object> keys, @NotNull CacheEnvironment<Object, T> env) {
        Map<Object, T> resultMap = new LinkedHashMap<>();
        Set<Object> missedKeys = new LinkedHashSet<>();
        for (Object key : keys) {
            byte[] bytes = map.get(key);
            resultMap.put(key, valueSerializer.deserialize(bytes));
            if (bytes == null) {
                missedKeys.add(key);
            }
        }
        Map<Object, T> loadedMap = env.getLoader().loadAll(missedKeys);
        for (Map.Entry<Object, T> e : resultMap.entrySet()) {
            if (e.getValue() == null) {
                e.setValue(loadedMap.get(e.getKey()));
            }
        }
        for (Object missedKey : missedKeys) {
            map.put(missedKey, valueSerializer.serialize(loadedMap.get(missedKey)));
        }
        return resultMap;
    }

    @Override
    public void deleteAll(@NotNull Collection<Object> keys, @Nullable Object reason) {
        map.keySet().removeAll(keys);
    }
}
