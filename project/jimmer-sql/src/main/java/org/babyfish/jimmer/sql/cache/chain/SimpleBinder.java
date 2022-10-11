package org.babyfish.jimmer.sql.cache.chain;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.NavigableMap;

public interface SimpleBinder<K, V> {

    @NotNull
    Map<K, V> getAll(@NotNull Collection<K> keys);

    void setAll(@NotNull Map<K, V> map);

    void deleteAll(@NotNull Collection<K> keys, @Nullable Object reason);

    interface Parameterized<K, V> extends SimpleBinder<K, V> {

        @Override
        @NotNull
        default Map<K, V> getAll(@NotNull Collection<K> keys) {
            return getAll(keys, Collections.emptyNavigableMap());
        }

        @Override
        default void setAll(@NotNull Map<K, V> map) {
            setAll(map, Collections.emptyNavigableMap());
        }

        @NotNull
        Map<K, V> getAll(
                @NotNull Collection<K> keys,
                @NotNull NavigableMap<String, Object> parameterMap
        );

        void setAll(
                @NotNull Map<K, V> map,
                @NotNull NavigableMap<String, Object> parameterMap
        );
    }
}
