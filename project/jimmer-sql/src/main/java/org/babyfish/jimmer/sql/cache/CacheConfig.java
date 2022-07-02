package org.babyfish.jimmer.sql.cache;

import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ImmutableProps;
import org.babyfish.jimmer.sql.ast.table.Table;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class CacheConfig {

    private CacheFactory cacheFactory;

    private final Map<ImmutableType, Cache<?, ?>> objectCacheMap =
            new HashMap<>();

    private final Map<ImmutableProp, Cache<?, ?>> associatedIdCacheMap =
            new HashMap<>();

    private final Map<ImmutableProp, Cache<?, List<?>>> associatedIdListCacheMap =
            new HashMap<>();

    @OldChain
    public CacheConfig setCacheFactory(CacheFactory cacheFactory) {
        this.cacheFactory = cacheFactory;
        return this;
    }

    @OldChain
    public <T> CacheConfig setObjectCache(
            Class<T> type,
            Cache<?, T> cache
    ) {
        ImmutableType immutableType = ImmutableType.get(type);
        objectCacheMap.put(immutableType, cache);
        return this;
    }

    @OldChain
    public <ST extends Table<?>> CacheConfig setAssociatedIdCache(
            Class<ST> sourceTableType,
            Function<ST, Table<?>> targetTableGetter,
            Cache<?, ?> cache
    ) {
        ImmutableProp prop = ImmutableProps.join(sourceTableType, targetTableGetter);
        CachesImpl.validateForAssociatedTargetId(prop);
        associatedIdCacheMap.put(prop, cache);
        return this;
    }

    @OldChain
    public <T, ST extends Table<?>, TT extends Table<T>> CacheConfig setAssociatedIdListCache(
            Class<ST> sourceTableType,
            Function<ST, Table<?>> targetTableGetter,
            Cache<?, List<?>> cache
    ) {
        ImmutableProp prop = ImmutableProps.join(sourceTableType, targetTableGetter);
        CachesImpl.validateForAssociationTargetIdList(prop);
        associatedIdListCacheMap.put(prop, cache);
        return this;
    }

    Caches build() {
        return new CachesImpl(
                cacheFactory,
                objectCacheMap,
                associatedIdCacheMap,
                associatedIdListCacheMap
        );
    }
}
