package org.babyfish.jimmer.sql.cache;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.meta.Column;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

class CachesImpl implements Caches {

    private final CacheFactory cacheFactory;

    private final Map<ImmutableType, Cache<?, ?>> objectCacheMap;

    private final Map<ImmutableProp, Cache<?, ?>> associatedIdCacheMap;

    private final Map<ImmutableProp, Cache<?, List<?>>> associatedIdListCacheMap;

    // ConcurrentHashMap.computeIfAbsent does not accept null value,
    // So read write lock is used here.
    private final ReadWriteLock rwl = new ReentrantReadWriteLock();

    CachesImpl(
            CacheFactory cacheFactory,
            Map<ImmutableType, Cache<?, ?>> objectCacheMap,
            Map<ImmutableProp, Cache<?, ?>> associatedIdCacheMap,
            Map<ImmutableProp, Cache<?, List<?>>> associatedIdListCacheMap
    ) {
        this.cacheFactory = cacheFactory;
        this.objectCacheMap = new HashMap<>(objectCacheMap);
        this.associatedIdCacheMap = new HashMap<>(associatedIdCacheMap);
        this.associatedIdListCacheMap = new HashMap<>(associatedIdListCacheMap);
    }

    @Override
    public <K, V> Cache<K, V> getObjectCache(ImmutableType type) {
        return CacheWrapper.wrap(getObjectCacheImpl(type), CacheWrapper.Type.OBJECT);
    }

    @Override
    public <K, V> Cache<K, V> getAssociatedIdCache(ImmutableProp prop) {
        return CacheWrapper.wrap(getAssociatedIdCacheImpl(prop), CacheWrapper.Type.ASSOCIATED_ID);
    }

    @Override
    public <K, V> Cache<K, List<V>> getAssociatedIdListCache(ImmutableProp prop) {
        return CacheWrapper.wrap(getAssociatedIdListCacheImpl(prop), CacheWrapper.Type.ASSOCIATED_ID_LIST);
    }

    @SuppressWarnings("unchecked")
    private <K, V> Cache<K, V> getObjectCacheImpl(ImmutableType type) {

        Lock lock;

        (lock = rwl.readLock()).lock();
        try {
            Cache<?, ?> cache = objectCacheMap.get(type);
            if (cache != null || objectCacheMap.containsKey(type)) {
                return (Cache<K, V>) cache;
            }
        } finally {
            lock.unlock();
        }

        (lock = rwl.writeLock()).lock();
        try {
            Cache<?, ?> cache = objectCacheMap.get(type);
            if (cache != null || objectCacheMap.containsKey(type)) {
                return (Cache<K, V>) cache;
            }
            if (cacheFactory != null) {
                cache = cacheFactory.createObjectCache(type);
            }
            objectCacheMap.put(type, cache);
            return (Cache<K, V>) cache;
        } finally {
            lock.unlock();
        }
    }

    @SuppressWarnings("unchecked")
    private <K, V> Cache<K, V> getAssociatedIdCacheImpl(ImmutableProp prop) {

        Lock lock;

        (lock = rwl.readLock()).lock();
        try {
            Cache<?, ?> cache = associatedIdCacheMap.get(prop);
            if (cache != null || associatedIdCacheMap.containsKey(prop)) {
                return (Cache<K, V>) cache;
            }
        } finally {
            lock.unlock();
        }

        (lock = rwl.writeLock()).lock();
        try {
            Cache<?, ?> cache = associatedIdCacheMap.get(prop);
            if (cache != null || associatedIdCacheMap.containsKey(prop)) {
                return (Cache<K, V>) cache;
            }
            validateForAssociatedTargetId(prop);
            if (cacheFactory != null) {
                cache = cacheFactory.createAssociatedIdCache(prop);
            }
            associatedIdCacheMap.put(prop, cache);
            return (Cache<K, V>) cache;
        } finally {
            lock.unlock();
        }
    }

    @SuppressWarnings("unchecked")
    private <K, V> Cache<K, List<V>> getAssociatedIdListCacheImpl(ImmutableProp prop) {

        Lock lock;

        (lock = rwl.readLock()).lock();
        try {
            Cache<?, ?> cache = associatedIdListCacheMap.get(prop);
            if (cache != null || associatedIdListCacheMap.containsKey(prop)) {
                return (Cache<K, List<V>>) cache;
            }
        } finally {
            lock.unlock();
        }

        (lock = rwl.writeLock()).lock();
        try {
            Cache<?, ?> cache = associatedIdListCacheMap.get(prop);
            if (cache != null || associatedIdListCacheMap.containsKey(prop)) {
                return (Cache<K, List<V>>) cache;
            }
            validateForAssociationTargetIdList(prop);
            if (cacheFactory != null) {
                cache = cacheFactory.createAssociatedIdListCache(prop);
            }
            associatedIdListCacheMap.put(prop, (Cache<?, List<?>>)cache);
            return (Cache<K, List<V>>) cache;
        } finally {
            lock.unlock();
        }
    }

    public static void validateForAssociatedTargetId(ImmutableProp prop) {
        if (!prop.isReference()) {
            throw new IllegalArgumentException(
                    "\"" + prop + "\" is not reference association"
            );
        }
    }

    public static void validateForAssociationTargetIdList(ImmutableProp prop) {
        if (!prop.isEntityList()) {
            throw new IllegalArgumentException(
                    "\"" + prop + "\" is not list association"
            );
        }
    }

    private static final Object DISABLED = new Object();
}
