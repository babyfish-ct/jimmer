package org.babyfish.jimmer.impl.util;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
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
    
    private final Map<K, V> positiveMap = new HashMap<>();

    private final Map<K, Void> negativeMap;

    public StaticCache(Function<K, V> creator) {
        this(creator, true);
    }

    public StaticCache(Function<K, V> creator, boolean nullable) {
        this.creator = creator;
        this.negativeMap = nullable ? new LRUMap<>() : null;
    }
    
    public V get(K key) {

        V value;
        Lock lock;

        (lock = cacheLock.readLock()).lock();
        try {
            if (negativeMap != null && negativeMap.containsKey(key)) {
                return null;
            }
            value = positiveMap.get(key);
        } finally {
            lock.unlock();
        }

        if (value == null) {
            (lock = cacheLock.writeLock()).lock();
            try {
                return getWithoutLock(key);
            } finally {
                lock.unlock();
            }
        }
        return value;
    }

    protected final V getWithoutLock(K key) {
        if (negativeMap != null && negativeMap.containsKey(key)) {
            return null;
        }
        V value = positiveMap.get(key);
        if (value == null) {
            value = creator.apply(key);
            if (value != null) {
                positiveMap.put(key, value);
                try {
                    onCreated(key, value);
                } catch (RuntimeException | Error ex) {
                    positiveMap.remove(key);
                    throw ex;
                }
            } else if (negativeMap != null) {
                negativeMap.put(key, null);
            } else {
                throw new IllegalStateException(
                        "The creator cannot return null because current static cache does not accept null values"
                );
            }
        }
        return value;
    }

    protected void onCreated(K key, V value) {}
}
