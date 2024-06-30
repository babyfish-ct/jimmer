package org.babyfish.jimmer.sql.common;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.cache.Cache;
import org.babyfish.jimmer.sql.cache.CacheEnvironment;
import org.babyfish.jimmer.sql.cache.ValueSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class CacheImpl<T> implements Cache<Object, T> {

    private final Map<Object, byte[]> map = new HashMap<>();

    private final ValueSerializer<T> valueSerializer;

    private final String logPrefix;

    private final Consumer<Collection<String>> onDelete;

    public CacheImpl(ImmutableType type) {
        valueSerializer = new ValueSerializer<>(type);
        logPrefix = null;
        onDelete = null;
    }

    public CacheImpl(ImmutableProp prop) {
        valueSerializer = new ValueSerializer<>(prop);
        logPrefix = null;
        this.onDelete = null;
    }

    public CacheImpl(ImmutableProp prop, Consumer<Collection<String>> onDelete) {
        valueSerializer = new ValueSerializer<>(prop);
        logPrefix = prop.getDeclaringType().getJavaClass().getSimpleName() + '.' + prop.getName() + '-';
        this.onDelete = onDelete;
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
        if (onDelete != null) {
            onDelete.accept(keys.stream().map(it -> logPrefix + it).collect(Collectors.toList()));
        }
        map.keySet().removeAll(keys);
    }
}
