package org.babyfish.jimmer.sql.cache;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;

public class CacheConfig {

    public CacheConfig setTypeName(ImmutableType immutableType, String typeName) {
        return this;
    }

    public CacheConfig setCache(CacheBinder binder, CacheLoader<?, ?> loader) {
        return setCache(binder, loader, null);
    }

    public CacheConfig setCache(CacheBinder binder, CacheLoader<?, ?> loader, CacheLocker locker) {
        return this;
    }

    public CacheConfig setCache(
            ImmutableType immutableType,
            CacheBinder binder,
            CacheLoader<?, ?> loader
    ) {
        return this.setCache(immutableType, binder, loader, null);
    }

    public CacheConfig setCache(
            ImmutableType immutableType,
            CacheBinder binder,
            CacheLoader<?, ?> loader,
            CacheLocker locker
    ) {
        return this;
    }

    public CacheConfig setCache(
            ImmutableProp prop,
            CacheBinder binder,
            CacheLoader<?, ?> loader
    ) {
        return this.setCache(prop, binder, loader, null);
    }

    public CacheConfig setCache(
            ImmutableProp prop,
            CacheBinder binder,
            CacheLoader<?, ?> loader,
            CacheLocker locker
    ) {
        return this;
    }
}
