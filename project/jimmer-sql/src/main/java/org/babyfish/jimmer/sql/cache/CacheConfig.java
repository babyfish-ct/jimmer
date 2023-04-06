package org.babyfish.jimmer.sql.cache;

import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.runtime.EntityManager;
import org.babyfish.jimmer.sql.event.Triggers;
import org.babyfish.jimmer.sql.ast.table.Table;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CacheConfig {

    private final EntityManager entityManager;

    private final String microServiceName;

    private final Map<ImmutableType, Cache<?, ?>> objectCacheMap =
            new LinkedHashMap<>();

    private final Map<ImmutableProp, Cache<?, ?>> propCacheMap =
            new LinkedHashMap<>();

    private CacheOperator operator;

    private CacheAbandonedCallback abandonedCallback;

    public CacheConfig(EntityManager entityManager, String microServiceName) {
        this.entityManager = entityManager;
        this.microServiceName = microServiceName;
    }

    @OldChain
    public CacheConfig setCacheFactory(CacheFactory cacheFactory) {
        if (entityManager == null) {
            throw new IllegalStateException("EntityManager must be set before set cache factory");
        }
        if (entityManager.getAllTypes(microServiceName).isEmpty()) {
            throw new IllegalStateException("vararg \"entityTypes\" cannot be empty");
        }
        if (cacheFactory == null) {
            throw new IllegalArgumentException("cacheFactory cannot bee null");
        }
        for (ImmutableType type : entityManager.getAllTypes(microServiceName)) {
            if (type.isEntity()) {
                if (!objectCacheMap.containsKey(type)) {
                    Cache<?, ?> objectCache = cacheFactory.createObjectCache(type);
                    if (objectCache != null) {
                        if (objectCache instanceof Cache.Parameterized<?, ?>) {
                            throw new IllegalStateException(
                                    "CacheFactory returns illegal cache for \"" +
                                            type +
                                            "\", object cache cannot be parameterized"
                            );
                        }
                        objectCacheMap.put(type, objectCache);
                    }
                }
                for (ImmutableProp prop : type.getProps().values()) {
                    if (propCacheMap.containsKey(prop)) {
                        continue;
                    }
                    if (prop.isRemote() && prop.getMappedBy() != null) {
                        continue;
                    }
                    if (prop.isAssociation(TargetLevel.ENTITY) || prop.hasTransientResolver()) {
                        Cache<?, ?> propCache =
                                prop.hasTransientResolver() ?
                                        cacheFactory.createResolverCache(prop) : (
                                        prop.isReferenceList(TargetLevel.ENTITY) ?
                                                cacheFactory.createAssociatedIdListCache(prop) :
                                                cacheFactory.createAssociatedIdCache(prop)
                                );
                        if (propCache != null) {
                            propCacheMap.put(prop, propCache);
                        }
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
        if (cache instanceof Cache.Parameterized<?, ?>) {
            throw new IllegalArgumentException(
                    "Object cache cannot be parameterized cache"
            );
        }
        ImmutableType immutableType = ImmutableType.get(type);
        if (!immutableType.getMicroServiceName().equals(microServiceName)) {
            throw new IllegalArgumentException(
                    "Cannot set object cache for \"" +
                            immutableType +
                            "\", it belongs to micro service \"" +
                            immutableType.getMicroServiceName() +
                            "\", not \"" +
                            microServiceName +
                            "\""
            );
        }
        objectCacheMap.put(immutableType, LocatedCacheImpl.unwrap(cache));
        return this;
    }

    @OldChain
    public <ST extends Table<?>> CacheConfig setAssociatedIdCache(
            TypedProp.Reference<?, ?> prop,
            Cache<?, ?> cache
    ) {
        return setAssociatedIdCache(prop.unwrap(), cache);
    }

    @OldChain
    public CacheConfig setAssociatedIdCache(
            ImmutableProp prop,
            Cache<?, ?> cache
    ) {
        validateProp(prop, false);
        propCacheMap.put(prop, LocatedCacheImpl.unwrap(cache));
        return this;
    }

    @OldChain
    public <T, ST extends Table<?>, TT extends Table<T>> CacheConfig setAssociatedIdListCache(
            TypedProp.ReferenceList<?, ?> prop,
            Cache<?, List<?>> cache
    ) {
        return setAssociatedIdListCache(prop.unwrap(), cache);
    }

    @OldChain
    public CacheConfig setAssociatedIdListCache(
            ImmutableProp prop,
            Cache<?, List<?>> cache
    ) {
        validateProp(prop, true);
        propCacheMap.put(prop, LocatedCacheImpl.unwrap(cache));
        return this;
    }

    @OldChain
    public CacheConfig setCalculatedCache(
            TypedProp<?, ?> prop,
            Cache<?, ?> cache
    ) {
        return setCalculatedCache(prop.unwrap(), cache);
    }

    @OldChain
    public CacheConfig setCalculatedCache(
            ImmutableProp prop,
            Cache<?, ?> cache
    ) {
        if (!prop.hasTransientResolver()) {
            throw new IllegalArgumentException("The prop \"" + prop + "\" is transient");
        }
        if (!prop.getDeclaringType().isEntity()) {
            throw new IllegalArgumentException("The prop \"" + prop + "\" is not declared in entity");
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
    public CacheConfig setAbandonedCallback(CacheAbandonedCallback callback) {
        this.abandonedCallback = callback;
        return this;
    }

    Caches build(Triggers triggers) {
        for (ImmutableProp prop : propCacheMap.keySet()) {
            if (prop.isAssociation(TargetLevel.PERSISTENT) && !objectCacheMap.containsKey(prop.getTargetType())) {
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
                abandonedCallback
        );
    }

    private void validateProp(ImmutableProp prop, boolean collection) {
        if (prop.isTransient()) {
            throw new IllegalArgumentException("The prop \"" + prop + "\" is transient");
        }
        if (collection && !prop.isReferenceList(TargetLevel.ENTITY)) {
            throw new IllegalArgumentException("The prop \"" + prop + "\" is not entity list");
        }
        if (!collection && !prop.isReference(TargetLevel.ENTITY)) {
            throw new IllegalArgumentException("The prop \"" + prop + "\" is not entity reference");
        }
        if (!prop.getDeclaringType().isEntity()) {
            throw new IllegalArgumentException("The prop \"" + prop + "\" is not declared in entity");
        }
        if (!prop.getDeclaringType().getMicroServiceName().equals(microServiceName)) {
            throw new IllegalArgumentException(
                    "The declaring type of prop \"" +
                            prop +
                            "\" does not belongs to micro service \"" +
                            microServiceName +
                            "\""
            );
        }
        if (prop.isRemote() && prop.getMappedBy() != null) {
            throw new IllegalArgumentException(
                    "The prop \"" +
                            prop +
                            "\" cannot is remote reversed(with `mappedBy`) association"
            );
        }
    }
}
