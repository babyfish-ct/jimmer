package org.babyfish.jimmer.sql.cache;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.SortedMap;

public interface Cache<K, V> {

    @Nullable default V get(@NotNull K key, @NotNull CacheEnvironment<K, V> env) {
        return getAll(Collections.singleton(key), env).get(key);
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

    interface Parameterized<K, V> extends Cache<K, V> {

        @Nullable
        default V get(
                @NotNull K key,
                @NotNull SortedMap<String, Object> parameterMap,
                @NotNull CacheEnvironment<K, V> env
        ) {
            return getAll(Collections.singleton(key), parameterMap, env).get(key);
        }

        @NotNull
        Map<K, V> getAll(
                @NotNull Collection<K> keys,
                @NotNull SortedMap<String, Object> parameterMap,
                @NotNull CacheEnvironment<K, V> env
        );
    }
}
