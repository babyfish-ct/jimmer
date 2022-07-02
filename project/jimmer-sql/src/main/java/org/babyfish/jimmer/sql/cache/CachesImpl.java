package org.babyfish.jimmer.sql.cache;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;

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
    // So use read write lock here.
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

    @SuppressWarnings("unchecked")
    @Override
    public <K, V> Cache<K, V> getObjectCache(ImmutableType type) {

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
    @Override
    public <K, V> Cache<K, V> getAssociatedIdCache(ImmutableProp prop) {

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
            return (Cache<K, V>) cache;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public <K, V> Cache<K, List<V>> getAssociatedIdListCache(ImmutableProp prop) {
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
            return (Cache<K, List<V>>) cache;
        } finally {
            lock.unlock();
        }
    }

    public static void validateForAssociatedTargetId(ImmutableProp prop) {
        if (prop.isReference()) {
            throw new IllegalArgumentException(
                    "\"" + prop + "\" is not reference association"
            );
        }
        if (prop.getStorage() != null) {
            throw new IllegalArgumentException(
                    "\"" + prop + "\" is association based on foreign key, " +
                            "it's unnecessary to specify the associated id cache for it"
            );
        }
    }

    public static void validateForAssociationTargetIdList(ImmutableProp prop) {
        if (prop.isReference()) {
            throw new IllegalArgumentException(
                    "\"" + prop + "\" is not list association"
            );
        }
    }
}
