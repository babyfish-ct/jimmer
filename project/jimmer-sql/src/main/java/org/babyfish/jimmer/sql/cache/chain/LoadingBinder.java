package org.babyfish.jimmer.sql.cache.chain;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;

public interface LoadingBinder<K, V> {

    void initialize(@NotNull CacheChain<K, V> chain);

    @NotNull
    Map<K, V> getAll(@NotNull Collection<K> keys);

    void deleteAll(@NotNull Collection<K> keys, Object reason);
}
