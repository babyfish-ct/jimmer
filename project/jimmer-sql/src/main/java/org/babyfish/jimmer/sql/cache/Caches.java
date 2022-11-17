package org.babyfish.jimmer.sql.cache;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TypedProp;

public interface Caches {

    default <K, V> Cache<K, V> getObjectCache(Class<V> type) {
        return getObjectCache(ImmutableType.get(type));
    }

    <K, V> Cache<K, V> getObjectCache(ImmutableType type);

    default <K, V> Cache<K, V> getPropertyCache(TypedProp<?, ?> prop) {
        return getPropertyCache(prop.unwrap());
    }

    <K, V> Cache<K, V> getPropertyCache(ImmutableProp prop);

    CacheAbandonedCallback getAbandonedCallback();
}
