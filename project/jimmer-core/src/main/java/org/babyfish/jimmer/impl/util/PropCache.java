package org.babyfish.jimmer.impl.util;

import org.babyfish.jimmer.meta.ImmutableProp;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

/*
 * Fight with spring-dev-tools
 */
public class PropCache<V> {

    private final Function<ImmutableProp, V> creator;

    private final ReadWriteLock cacheLock = new ReentrantReadWriteLock();

    private final Map<ImmutableProp, V> positiveMap = new HashMap<>();

    private final Map<String, V> positiveMap2 = new HashMap<>();

    private final Map<ImmutableProp, Void> negativeMap;

    private final Map<String, Void> negativeMap2;

    public PropCache(Function<ImmutableProp, V> creator) {
        this(creator, false);
    }

    public PropCache(Function<ImmutableProp, V> creator, boolean nullable) {
        this.creator = creator;
        negativeMap = nullable ? new LRUMap<>() : null;
        negativeMap2 = nullable ? new LRUMap<>() : null;
    }

    public V get(ImmutableProp key) {

        String keyString = key != null ? key.toString() : null;
        V value;
        Lock lock;

        (lock = cacheLock.readLock()).lock();
        try {
            if (negativeMap != null && negativeMap.containsKey(key)) {
                return null;
            }
            if (negativeMap2 != null && negativeMap2.containsKey(keyString)) {
                negativeMap.put(key, null);
            }
            value = positiveMap.get(key);
            if (value == null) {
                value = positiveMap2.get(keyString);
                if (value != null) {
                    positiveMap.put(key, value);
                }
            }
        } finally {
            lock.unlock();
        }

        if (value == null) {
            (lock = cacheLock.writeLock()).lock();
            try {
                if (negativeMap != null && negativeMap.containsKey(key)) {
                    return null;
                }
                if (negativeMap2 != null && negativeMap2.containsKey(keyString)) {
                    negativeMap.put(key, null);
                }
                value = positiveMap.get(key);
                if (value == null) {
                    value = positiveMap2.get(keyString);
                    if (value != null) {
                        positiveMap.put(key, value);
                    } else {
                        value = creator.apply(key);
                        if (value != null) {
                            positiveMap.put(key, value);
                            positiveMap2.put(keyString, value);
                        } else if (negativeMap != null) {
                            negativeMap.put(key, null);
                            negativeMap2.put(keyString, null);
                        } else {
                            throw new IllegalStateException(
                                    "The creator cannot return null because current type cache does not accept null values"
                            );
                        }
                    }
                }
            } finally {
                lock.unlock();
            }
        }

        return value;
    }
}
