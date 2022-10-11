package org.babyfish.jimmer.sql.cache.chain;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.NavigableMap;

public interface CacheChain<K, V> {

    @NotNull
    Map<K, V> loadAll(@NotNull Collection<K> keys);

    interface Parameterized<K, V> {

        @NotNull
        Map<K, V> loadAll(
                @NotNull Collection<K> keys,
                @NotNull NavigableMap<String, Object> parameterMap
        );
    }
}