package org.babyfish.jimmer.sql.example.cache;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.cache.AbstractCacheFactory;
import org.babyfish.jimmer.sql.cache.Cache;
import org.babyfish.jimmer.sql.cache.chain.ChainCacheBuilder;
import org.babyfish.jimmer.sql.cache.chain.SimpleBinder;
import org.babyfish.jimmer.sql.example.Context;

import java.util.List;

public class CacheFactoryImpl extends AbstractCacheFactory implements Context {

    @Override
    public Cache<?, ?> createObjectCache(ImmutableType type) {
        return new ChainCacheBuilder<>()
                .add(new ObjectCacheBinder(CACHE_STORAGE.objectMap, type))
                .build();
    }

    @Override
    public Cache<?, ?> createAssociatedIdCache(ImmutableProp prop) {
        SimpleBinder<Object, Object> binder =
                getFilterState().isAffected(prop.getTargetType()) ?
                        new MultipleViewPropCacheBinder(CACHE_STORAGE.multiViewPropCache, prop) :
                        new SingleViewPropCacheBinder(CACHE_STORAGE.singleViewPropMap, prop);
        return new ChainCacheBuilder<>()
                .add(binder)
                .build();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Cache<?, List<?>> createAssociatedIdListCache(ImmutableProp prop) {
        SimpleBinder<?, ?> binder =
                getFilterState().isAffected(prop.getTargetType()) ?
                new MultipleViewPropCacheBinder(CACHE_STORAGE.multiViewPropCache, prop) :
                new SingleViewPropCacheBinder(CACHE_STORAGE.singleViewPropMap, prop);
        return new ChainCacheBuilder<Object, List<?>>()
                .add((SimpleBinder<Object, List<?>>) binder)
                .build();
    }
}
