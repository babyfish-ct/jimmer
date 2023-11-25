package org.babyfish.jimmer.sql.meta.impl;

import org.babyfish.jimmer.sql.meta.SqlContext;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

/*
 * Often there is only ONE key,
 * so ConcurrentHashMap is not cost-effective.
 */
public class SqlContextCache<T> {

    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    private final Function<SqlContext, T> creator;

    private SqlContext primaryKey;

    private T primaryValue;

    private Map<SqlContext, T> otherMap;

    public SqlContextCache(Function<SqlContext, T> creator) {
        this.creator = creator;
    }

    public T get(SqlContext strategy) {

        SqlContext unwrapped = strategy.unwrap();
        if (unwrapped != null) {
            strategy = unwrapped;
        }

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
                Map<SqlContext, T> om = otherMap;
                if (om != null && om.size() > 512) {
                    otherMap = om = null;
                }
                value = strategy.equals(primaryKey) ?
                        primaryValue :
                        om != null ?
                                om.get(strategy) :
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
                        if (om == null) {
                            otherMap = om = new WeakHashMap<>();
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
