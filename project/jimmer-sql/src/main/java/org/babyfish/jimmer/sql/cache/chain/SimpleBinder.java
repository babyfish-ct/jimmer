package org.babyfish.jimmer.sql.cache.chain;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;

public interface SimpleBinder<K, V> {

    @NotNull
    Map<K, V> getAll(@NotNull Collection<K> keys);

    void setAll(@NotNull Map<K, V> map);

    void deleteAll(@NotNull Collection<K> keys);
}
