package org.babyfish.jimmer.sql.cache;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public interface Cache<K, V> {

    @NotNull default Optional<V> get(@NotNull K key, @NotNull CacheEnvironment<K, V> env) {
        Map<K, V> map = getAll(Collections.singleton(key), env);
        V value = map.get(key);
        if (value == null && map.containsKey(key)) {
            return null;
        }
        return Optional.ofNullable(value);
    }

    @NotNull Map<K, V> getAll(@NotNull Collection<K> keys, @NotNull CacheEnvironment<K, V> env);

    default void delete(@NotNull K key) {
        deleteAll(Collections.singleton(key), null);
    }

    default void delete(@NotNull K key, Object reason) {
        deleteAll(Collections.singleton(key), reason);
    }

    default void deleteAll(@NotNull Collection<K> keys) {
        deleteAll(keys, null);
    }

    void deleteAll(@NotNull Collection<K> keys, @Nullable Object reason);
}
