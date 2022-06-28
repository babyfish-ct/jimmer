package org.babyfish.jimmer.sql.cache;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;

public class CacheConfig {

    public CacheConfig setTypeName(ImmutableType immutableType, String typeName) {
        return this;
    }

    public CacheConfig setCache(CacheProvider<?> provider, CacheLoader<?, ?> loader) {
        return setCache(provider, loader, null);
    }

    public CacheConfig setCache(CacheProvider<?> provider, CacheLoader<?, ?> loader, CacheLocker locker) {
        return this;
    }

    public CacheConfig setCache(
            ImmutableType immutableType,
            CacheProvider<?> binder,
            CacheLoader<?, ?> loader
    ) {
        return this.setCache(immutableType, binder, loader, null);
    }

    public CacheConfig setCache(
            ImmutableType immutableType,
            CacheProvider<?> provider,
            CacheLoader<?, ?> loader,
            CacheLocker locker
    ) {
        return this;
    }

    public CacheConfig setCache(
            ImmutableProp prop,
            CacheProvider<?> provider,
            CacheLoader<?, ?> loader
    ) {
        return this.setCache(prop, provider, loader, null);
    }

    public CacheConfig setCache(
            ImmutableProp prop,
            CacheProvider<?> provider,
            CacheLoader<?, ?> loader,
            CacheLocker locker
    ) {
        return this;
    }
}
