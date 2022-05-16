package org.babyfish.jimmer.util;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

public class OptionalValueCache<K, V> {

    private Function<K, V> creator;

    private ReadWriteLock cacheLock = new ReentrantReadWriteLock();
    
    private Map<K, V> positiveCacheMap = new HashMap<>();

    private Map<K, Void> negativeCacheMap = new LRUMap<>();

    public OptionalValueCache(Function<K, V> creator) {
        this.creator = creator;
    }
    
    public V get(K key) {

        V value;
        Lock lock;

        (lock = cacheLock.readLock()).lock();
        try {
            if (negativeCacheMap.containsKey(key)) {
                return null;
            }
            value = positiveCacheMap.get(key);
        } finally {
            lock.unlock();
        }

        if (value == null) {
            (lock = cacheLock.writeLock()).lock();
            try {
                if (negativeCacheMap.containsKey(key)) {
                    return null;
                }
                value = positiveCacheMap.get(key);
                if (value == null) {
                    value = creator.apply(key);
                    if (value != null) {
                        positiveCacheMap.put(key, value);
                    } else {
                        negativeCacheMap.put(key, null);
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
