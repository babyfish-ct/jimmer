package org.babyfish.jimmer.sql.cache;

import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.filter.impl.FilterManager;
import org.babyfish.jimmer.sql.meta.JoinTemplate;
import org.babyfish.jimmer.sql.event.Triggers;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.runtime.EntityManager;

import java.util.*;

public class CacheConfig {

    private CacheFactory cacheFactory;

    private final Map<ImmutableType, Cache<?, ?>> objectCacheMap =
            new LinkedHashMap<>();

    private final Map<ImmutableProp, Cache<?, ?>> propCacheMap =
            new LinkedHashMap<>();

    private CacheOperator operator;

    private Set<CacheAbandonedCallback> abandonedCallbacks = new LinkedHashSet<>();

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
        if (cache instanceof Cache.Parameterized<?, ?>) {
            throw new IllegalArgumentException(
                    "Object cache cannot be parameterized cache"
            );
        }
        ImmutableType immutableType = ImmutableType.get(type);
        objectCacheMap.put(immutableType, cache);
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
        validateAssociationProp(prop, false);
        propCacheMap.put(prop, cache);
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
        validateAssociationProp(prop, true);
        propCacheMap.put(prop, cache);
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
        propCacheMap.put(prop, cache);
        return this;
    }

    @OldChain
    public CacheConfig setCacheOperator(CacheOperator operator) {
        this.operator = operator;
        return this;
    }

    @OldChain
    public CacheConfig addAbandonedCallback(CacheAbandonedCallback callback) {
        abandonedCallbacks.add(callback);
        return this;
    }

    @OldChain
    public CacheConfig addAbandonedCallbacks(Collection<? extends CacheAbandonedCallback> callbacks) {
        abandonedCallbacks.addAll(callbacks);
        return this;
    }

    private void validateAssociationProp(ImmutableProp prop, boolean collection) {
        if (prop.isTransient()) {
            throw new IllegalArgumentException("The prop \"" + prop + "\" is transient");
        }
        if (prop.getSqlTemplate() instanceof JoinTemplate) {
            throw new IllegalArgumentException(
                    "The prop \"" +
                            prop +
                            "\" is decorated by \"@" +
                            JoinTemplate.class.getName() +
                            "\" which does not support cache"
            );
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
        if (prop.isRemote() && prop.getMappedBy() != null) {
            throw new IllegalArgumentException(
                    "The prop \"" +
                            prop +
                            "\" cannot is remote reversed(with `mappedBy`) association"
            );
        }
    }

    Caches build(
            String microServiceName,
            EntityManager entityManager,
            Triggers triggers,
            FilterManager filterManager
    ) {

        CacheFactory cacheFactory = this.cacheFactory;
        Map<ImmutableType, Cache<?, ?>> objectCacheMap = this.objectCacheMap;
        Map<ImmutableProp, Cache<?, ?>> propCacheMap = this.propCacheMap;

        Map<ImmutableType, Cache<?, ?>> finalObjectCacheMap = new LinkedHashMap<>();
        Map<ImmutableProp, Cache<?, ?>> finalPropCacheMap = new LinkedHashMap<>();

        if (cacheFactory instanceof FilterStateAware) {
            Set<ImmutableType> affectedTypes =
                    filterManager.getAffectedTypes(
                            entityManager.getAllTypes(microServiceName)
                    );
            ((FilterStateAware)cacheFactory).setFilterState(affectedTypes::contains);
        }

        for (ImmutableType type : entityManager.getAllTypes(microServiceName)) {
            if (type.isEntity()) {
                Cache<?, ?> finalObjectCache = objectCacheMap.get(type);
                if (finalObjectCache == null && cacheFactory != null) {
                    finalObjectCache = cacheFactory.createObjectCache(type);
                    if (finalObjectCache != null) {
                        if (finalObjectCache instanceof Cache.Parameterized<?, ?>) {
                            throw new IllegalStateException(
                                    "CacheFactory returns illegal cache for \"" +
                                            type +
                                            "\", object cache cannot be parameterized"
                            );
                        }
                    }
                }
                if (finalObjectCache != null) {
                    finalObjectCacheMap.put(type, finalObjectCache);
                }
                for (ImmutableProp prop : type.getProps().values()) {
                    Cache<?, ?> finalPropCache = propCacheMap.get(prop);
                    if (finalPropCache == null &&
                            cacheFactory != null &&
                            (!prop.isRemote() || prop.getMappedBy() == null) &&
                            prop.getSqlTemplate() == null &&
                            (prop.isAssociation(TargetLevel.ENTITY) || prop.hasTransientResolver())
                    ) {
                        finalPropCache =
                                prop.hasTransientResolver() ?
                                        cacheFactory.createResolverCache(prop) : (
                                        prop.isReferenceList(TargetLevel.ENTITY) ?
                                                cacheFactory.createAssociatedIdListCache(prop) :
                                                cacheFactory.createAssociatedIdCache(prop)
                                );
                    }
                    if (finalPropCache != null) {
                        finalPropCacheMap.put(prop, finalPropCache);
                    }
                }
            }
        }
        for (ImmutableProp prop : finalPropCacheMap.keySet()) {
            if (prop.isAssociation(TargetLevel.PERSISTENT) && !finalObjectCacheMap.containsKey(prop.getTargetType())) {
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
                finalObjectCacheMap,
                finalPropCacheMap,
                operator,
                CompositeCacheAbandonedCallback.combine(abandonedCallbacks)
        );
    }
}
