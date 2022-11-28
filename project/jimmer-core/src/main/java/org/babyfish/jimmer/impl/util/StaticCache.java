package org.babyfish.jimmer.impl.util;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

/**
 * Internal until class, it should not be used by programmer directly.
 *
 * @param <K> Key Type
 * @param <V> Value Type
 */
public class StaticCache<K, V> {

    private final Function<K, V> creator;

    private final ReadWriteLock cacheLock = new ReentrantReadWriteLock();
    
    private final Map<K, V> positiveCacheMap = new HashMap<>();

    private Map<K, Void> negativeCacheMap;

    public StaticCache(Function<K, V> creator) {
        this(creator, true);
    }

    public StaticCache(Function<K, V> creator, boolean nullable) {
        this.creator = creator;
        if (nullable) {
             negativeCacheMap = new LRUMap<>();
        }
    }
    
    public V get(K key) {

        V value;
        Lock lock;

        (lock = cacheLock.readLock()).lock();
        try {
            if (negativeCacheMap != null && negativeCacheMap.containsKey(key)) {
                return null;
            }
            value = positiveCacheMap.get(key);
        } finally {
            lock.unlock();
        }

        if (value == null) {
            (lock = cacheLock.writeLock()).lock();
            try {
                if (negativeCacheMap != null && negativeCacheMap.containsKey(key)) {
                    return null;
                }
                value = positiveCacheMap.get(key);
                if (value == null) {
                    value = creator.apply(key);
                    if (value != null) {
                        positiveCacheMap.put(key, value);
                    } else if (negativeCacheMap != null) {
                        negativeCacheMap.put(key, null);
                    } else {
                        throw new IllegalStateException(
                                "The creator cannot return null because current static cache does not accept null values"
                        );
                    }
                }
            } finally {
                lock.unlock();
            }
        }
        return value;
    }

    private static class LRUMap<K, V> extends LinkedHashMap<K, V> {

        LRUMap() {
            super((128 * 4 + 2) / 3, .75F, true);
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry eldest) {
            return true;
        }
    }
}
