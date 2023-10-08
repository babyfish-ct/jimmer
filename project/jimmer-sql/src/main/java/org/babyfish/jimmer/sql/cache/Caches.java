package org.babyfish.jimmer.sql.cache;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.event.DatabaseEvent;
import org.jetbrains.annotations.Nullable;

public interface Caches {

    @Nullable
    default <K, V> Cache<K, V> getObjectCache(Class<V> type) {
        return getObjectCache(ImmutableType.get(type));
    }

    @Nullable
    <K, V> Cache<K, V> getObjectCache(ImmutableType type);

    @Nullable
    default <K, V> Cache<K, V> getPropertyCache(TypedProp<?, ?> prop) {
        return getPropertyCache(prop.unwrap());
    }

    @Nullable
    <K, V> Cache<K, V> getPropertyCache(ImmutableProp prop);

    CacheAbandonedCallback getAbandonedCallback();

    boolean isAffectedBy(DatabaseEvent e);
}
