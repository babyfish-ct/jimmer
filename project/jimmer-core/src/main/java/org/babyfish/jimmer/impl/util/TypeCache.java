package org.babyfish.jimmer.impl.util;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.spi.ImmutableTypeImplementor;

import java.util.function.Function;

public class TypeCache<V> {

    private final Function<ImmutableType, V> creator;

    private final boolean nullable;

    private final ClassCache<CacheSlots<ImmutableType, V>> classCache;

    public TypeCache(Function<ImmutableType, V> creator) {
        this(creator, false);
    }

    public TypeCache(Function<ImmutableType, V> creator, boolean nullable) {
        this.creator = creator;
        this.nullable = nullable;
        classCache = new ClassCache<>(this::createSlots);
    }

    public V get(ImmutableType key) {
        ImmutableTypeImplementor implementor = (ImmutableTypeImplementor) key;
        ImmutableType cacheOwnerType = implementor.getCacheOwnerType();
        return classCache
                .get(cacheOwnerType.getJavaClass())
                .get(key, implementor.getTypeCacheSlot());
    }

    private CacheSlots<ImmutableType, V> createSlots(Class<?> javaClass) {
        ImmutableTypeImplementor type = (ImmutableTypeImplementor) ImmutableType.get(javaClass);
        return new CacheSlots<>(type.getTypeCacheSlotCount(), creator, nullable);
    }
}
