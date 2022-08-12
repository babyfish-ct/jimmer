package org.babyfish.jimmer.sql.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ImmutableProps;
import org.babyfish.jimmer.sql.Triggers;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.event.binlog.BinLogParser;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public class CacheConfig {

    private final Map<ImmutableType, Cache<?, ?>> objectCacheMap =
            new LinkedHashMap<>();

    private final Map<ImmutableProp, Cache<?, ?>> propCacheMap =
            new LinkedHashMap<>();

    private CacheOperator operator;

    private ObjectMapper binLogObjectMapper;

    @OldChain
    public CacheConfig setCacheFactory(Class<?>[] entityTypes, CacheFactory cacheFactory) {
        if (entityTypes.length == 0) {
            throw new IllegalArgumentException("vararg \"entityTypes\" cannot be empty");
        }
        if (cacheFactory == null) {
            throw new IllegalArgumentException("cacheFactory cannot bee null");
        }
        for (Class<?> entityType : entityTypes) {
            ImmutableType type = ImmutableType.get(entityType);
            if (!objectCacheMap.containsKey(type)) {
                Cache<?, ?> objectCache = cacheFactory.createObjectCache(type);
                if (objectCache != null) {
                    objectCacheMap.put(type, objectCache);
                }
            }
            for (ImmutableProp prop : type.getProps().values()) {
                if ((prop.isAssociation() || prop.hasTransientResolver()) && !propCacheMap.containsKey(prop)) {
                    Cache<?, ?> propCache =
                            prop.hasTransientResolver() ?
                                    cacheFactory.createResolverCache(prop) : (
                                            prop.isEntityList() ?
                                                    cacheFactory.createAssociatedIdListCache(prop) :
                                                    cacheFactory.createAssociatedIdCache(prop)
                                    );
                    if (propCache != null) {
                        propCacheMap.put(prop, propCache);
                    }
                }
            }
        }
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
        propCacheMap.put(prop, LocatedCacheImpl.unwrap(cache));
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
        propCacheMap.put(prop, LocatedCacheImpl.unwrap(cache));
        return this;
    }

    @OldChain
    public CacheConfig setResolverCache(
            ImmutableProp prop,
            Cache<?, ?> cache
    ) {
        if (!prop.hasTransientResolver()) {
            throw new IllegalArgumentException("The prop \"" + prop + "\" is transient property with resolver");
        }
        propCacheMap.put(prop, LocatedCacheImpl.unwrap(cache));
        return this;
    }

    @OldChain
    public CacheConfig setCacheOperator(CacheOperator operator) {
        this.operator = operator;
        return this;
    }

    @OldChain
    public CacheConfig setBinLogObjectMapper(ObjectMapper objectMapper) {
        this.binLogObjectMapper = objectMapper;
        return this;
    }

    Caches build(Triggers triggers) {
        for (ImmutableProp prop : propCacheMap.keySet()) {
            if (prop.isAssociation() && !objectCacheMap.containsKey(prop.getTargetType())) {
                throw new IllegalStateException(
                        "The cache for association property \"" +
                                prop +
                                "\" is configured but there is no cache for the target type \"" +
                                prop.getTargetType() +
                                "\""
                );
            }
        }
        return new CachesImpl(
                triggers,
                objectCacheMap,
                propCacheMap,
                operator,
                new BinLogParser(binLogObjectMapper)
        );
    }
}
