package org.babyfish.jimmer.sql.cache;

import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ImmutableProps;
import org.babyfish.jimmer.sql.Triggers;
import org.babyfish.jimmer.sql.ast.table.Table;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class CacheConfig {

    private CacheFactory cacheFactory;

    private final Map<ImmutableType, Cache<?, ?>> objectCacheMap =
            new HashMap<>();

    private final Map<ImmutableProp, Cache<?, ?>> associationCacheMap =
            new HashMap<>();

    private CacheOperator operator;

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
        objectCacheMap.put(immutableType, LocatedCacheImpl.unwrap(cache));
        return this;
    }

    @OldChain
    public <ST extends Table<?>> CacheConfig setAssociatedIdCache(
            Class<ST> sourceTableType,
            Function<ST, Table<?>> targetTableGetter,
            Cache<?, ?> cache
    ) {
        ImmutableProp prop = ImmutableProps.join(sourceTableType, targetTableGetter);
        return setAssociatedIdCache(prop, cache);
    }

    @OldChain
    public CacheConfig setAssociatedIdCache(
            ImmutableProp prop,
            Cache<?, ?> cache
    ) {
        if (!prop.isReference()) {
            throw new IllegalArgumentException("The prop \"" + prop + "\" is not reference");
        }
        if (!prop.isNullable()) {
            throw new IllegalArgumentException(
                    "Cannot set cache for \"" + prop + "\", " +
                            "non-null reference association does not support cache"
            );
        }
        associationCacheMap.put(prop, LocatedCacheImpl.unwrap(cache));
        return this;
    }

    @OldChain
    public <T, ST extends Table<?>, TT extends Table<T>> CacheConfig setAssociatedIdListCache(
            Class<ST> sourceTableType,
            Function<ST, Table<?>> targetTableGetter,
            Cache<?, List<?>> cache
    ) {
        ImmutableProp prop = ImmutableProps.join(sourceTableType, targetTableGetter);
        return setAssociatedIdListCache(prop, cache);
    }

    @OldChain
    public CacheConfig setAssociatedIdListCache(
            ImmutableProp prop,
            Cache<?, List<?>> cache
    ) {
        if (!prop.isReference()) {
            throw new IllegalArgumentException("The prop \"" + prop + "\" is not list");
        }
        associationCacheMap.put(prop, LocatedCacheImpl.unwrap(cache));
        return this;
    }

    @OldChain
    public CacheConfig setCacheOperator(CacheOperator operator) {
        this.operator = operator;
        return this;
    }

    Caches build(Triggers triggers) {
        return new CachesImpl(
                triggers,
                cacheFactory,
                objectCacheMap,
                associationCacheMap,
                operator
        );
    }
}
