package org.babyfish.jimmer.sql.meta.impl;

import org.babyfish.jimmer.sql.meta.MetadataStrategy;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

/*
 * Often there is only ONE key,
 * so ConcurrentHashMap is not cost-effective.
 */
public class MetaCache<T> {

    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    private final Function<MetadataStrategy, T> creator;

    private MetadataStrategy primaryKey;

    private T primaryValue;

    private Map<MetadataStrategy, T> otherMap;

    public MetaCache(Function<MetadataStrategy, T> creator) {
        this.creator = creator;
    }

    public T get(MetadataStrategy strategy) {

        Lock lock;
        T value;

        (lock = readWriteLock.readLock()).lock();
        try {
            value = strategy.equals(primaryKey) ?
                    primaryValue :
                    otherMap != null ?
                            otherMap.get(strategy) :
                            null;
        } finally {
            lock.unlock();
        }

        if (value == null) {
            (lock = readWriteLock.writeLock()).lock();
            try {
                value = strategy.equals(primaryKey) ?
                        primaryValue :
                        otherMap != null ?
                                otherMap.get(strategy) :
                                null;
                if (value == null) {
                    value = creator.apply(strategy);
                    if (value == null) {
                        throw new AssertionError("Internal bug: creator of MetaCache cannot return null");
                    }
                    if (primaryKey == null) {
                        primaryKey = strategy;
                        primaryValue = value;
                    } else {
                        Map<MetadataStrategy, T> om = otherMap;
                        if (om == null) {
                            otherMap = om = new HashMap<>();
                        }
                        om.put(strategy, value);
                    }
                }
            } finally {
                lock.unlock();
            }
        }

        return value;
    }
}
