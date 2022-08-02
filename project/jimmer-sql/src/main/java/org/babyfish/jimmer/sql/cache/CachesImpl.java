package org.babyfish.jimmer.sql.cache;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.Triggers;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class CachesImpl implements Caches {

    private final Map<ImmutableType, LocatedCacheImpl<?, ?>> objectCacheMap;

    private final Map<ImmutableProp, LocatedCacheImpl<?, ?>> associationCacheMap;

    private final CacheOperator operator;

    private final boolean disableAll;

    private final Set<ImmutableType> disabledTypes;

    private final Set<ImmutableProp> disabledProps;

    public CachesImpl(
            Triggers triggers,
            Map<ImmutableType, Cache<?, ?>> objectCacheMap,
            Map<ImmutableProp, Cache<?, ?>> associationCacheMap,
            CacheOperator operator
    ) {
        Map<ImmutableType, LocatedCacheImpl<?, ?>> objectCacheWrapperMap = new LinkedHashMap<>();
        for (Map.Entry<ImmutableType, Cache<?, ?>> e : objectCacheMap.entrySet()) {
            ImmutableType type = e.getKey();
            objectCacheWrapperMap.put(type, wrapObjectCache(triggers, e.getValue(), type));
        }
        Map<ImmutableProp, LocatedCacheImpl<?, ?>> associationCacheWrapperMap = new LinkedHashMap<>();
        for (Map.Entry<ImmutableProp, Cache<?, ?>> e : associationCacheMap.entrySet()) {
            ImmutableProp prop = e.getKey();
            associationCacheWrapperMap.put(prop, wrapAssociationCache(triggers, e.getValue(), prop));
        }
        this.objectCacheMap = objectCacheWrapperMap;
        this.associationCacheMap = associationCacheWrapperMap;
        this.operator = operator;
        disableAll = false;
        disabledTypes = Collections.emptySet();
        disabledProps = Collections.emptySet();
    }

    public CachesImpl(
            CachesImpl base,
            CacheDisableConfig cfg
    ) {
        objectCacheMap = base.objectCacheMap;
        associationCacheMap = base.associationCacheMap;
        operator = base.operator;
        this.disableAll = cfg.isDisableAll();
        this.disabledTypes = cfg.getDisabledTypes();
        this.disabledProps = cfg.getDisabledProps();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <K, V> LocatedCache<K, V> getObjectCache(ImmutableType type) {
        if (disableAll || disabledTypes.contains(type)) {
            return null;
        }
        return LocatedCacheImpl.export((LocatedCache<K, V>)objectCacheMap.get(type));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <K, V> LocatedCache<K, V> getAssociationCache(ImmutableProp prop) {
        if (disableAll ||
                disabledProps.contains(prop) ||
                disabledTypes.contains(prop.getTargetType())
        ) {
            return null;
        }
        return LocatedCacheImpl.export((LocatedCache<K, V>)associationCacheMap.get(prop));
    }

    @SuppressWarnings("unchecked")
    private LocatedCacheImpl<?, ?> wrapObjectCache(
            Triggers triggers,
            Cache<?, ?> cache,
            ImmutableType type
    ) {
        if (cache == null) {
            return null;
        }
        LocatedCacheImpl<Object, Object> wrapper = LocatedCacheImpl.wrap(
                (Cache<Object, Object>) cache,
                type
        );
        triggers.addEntityListener(type, e -> {
            ImmutableSpi oldEntity = e.getOldEntity();
            if (oldEntity != null) {
                Object id = oldEntity.__get(type.getIdProp().getId());
                if (operator != null) {
                    operator.delete(wrapper, id);
                } else {
                    wrapper.delete(id);
                }
            }
        });
        return wrapper;
    }

    @SuppressWarnings("unchecked")
    private LocatedCacheImpl<?, ?> wrapAssociationCache(
            Triggers triggers,
            Cache<?, ?> cache,
            ImmutableProp prop
    ) {
        if (cache == null) {
            return null;
        }
        LocatedCacheImpl<Object, Object> wrapper = LocatedCacheImpl.wrap(
                (Cache<Object, Object>) cache,
                prop
        );
        triggers.addAssociationListener(prop, e -> {
            Object id = e.getSourceId();
            if (operator != null) {
                operator.delete(wrapper, id);
            } else {
                wrapper.delete(id);
            }
        });
        return wrapper;
    }
}
