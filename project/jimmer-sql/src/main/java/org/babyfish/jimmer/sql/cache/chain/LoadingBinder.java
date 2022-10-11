package org.babyfish.jimmer.sql.cache.chain;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.NavigableMap;

public interface LoadingBinder<K, V> {

    void initialize(@NotNull CacheChain<K, V> chain);

    @NotNull
    Map<K, V> getAll(@NotNull Collection<K> keys);

    void deleteAll(@NotNull Collection<K> keys, @Nullable Object reason);

    public interface Parameterized<K, V> {

        void initialize(@NotNull CacheChain.Parameterized<K, V> chain);

        @NotNull
        default Map<K, V> getAll(@NotNull Collection<K> keys) {
            return getAll(keys, Collections.emptyNavigableMap());
        }

        @NotNull Map<K, V> getAll(
                @NotNull Collection<K> keys,
                @NotNull NavigableMap<String, Object> parameterMap
        );

        void deleteAll(@NotNull Collection<K> keys, @Nullable Object reason);
    }
}
