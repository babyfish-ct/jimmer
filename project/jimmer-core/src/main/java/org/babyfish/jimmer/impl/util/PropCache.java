package org.babyfish.jimmer.impl.util;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.spi.ImmutablePropImplementor;
import org.babyfish.jimmer.meta.spi.ImmutableTypeImplementor;

import java.util.function.Function;

public class PropCache<V> {

    private final Function<ImmutableProp, V> creator;

    private final boolean nullable;

    private final ClassCache<CacheSlots<ImmutableProp, V>> classCache;

    public PropCache(Function<ImmutableProp, V> creator) {
        this(creator, false);
    }

    public PropCache(Function<ImmutableProp, V> creator, boolean nullable) {
        this.creator = creator;
        this.nullable = nullable;
        classCache = new ClassCache<>(this::createSlots);
    }

    public V get(ImmutableProp key) {
        ImmutablePropImplementor implementor = (ImmutablePropImplementor) key;
        ImmutableType cacheOwnerType = implementor.getCacheOwnerType();
        return classCache
                .get(cacheOwnerType.getJavaClass())
                .get(key, implementor.getPropCacheSlot());
    }

    private CacheSlots<ImmutableProp, V> createSlots(Class<?> javaClass) {
        ImmutableTypeImplementor type = (ImmutableTypeImplementor) ImmutableType.get(javaClass);
        return new CacheSlots<>(type.getPropCacheSlotCount(), creator, nullable);
    }
}
