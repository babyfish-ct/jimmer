package org.babyfish.jimmer.sql.meta.impl;

import org.babyfish.jimmer.sql.meta.MetadataStrategy;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/*
 * Often there is only ONE key,
 * so create the ConcurrentHashMap only when a secondary key is used.
 */
public class MetaCache<T> {

    private final Function<MetadataStrategy, T> creator;

    private final int maxCount;

    private volatile MetadataStrategy primaryKey;

    private T primaryValue;

    private volatile ConcurrentHashMap<MetadataStrategy, T> secondaryMap;

    public MetaCache(Function<MetadataStrategy, T> creator) {
        this.creator = creator;
        this.maxCount = -1;
    }

    public MetaCache(Function<MetadataStrategy, T> creator, int maxCount) {
        this.creator = creator;
        this.maxCount = maxCount;
    }

    public T get(MetadataStrategy strategy) {
        MetadataStrategy primaryKey = this.primaryKey;
        if (primaryKey != null) {
            if (strategy.equals(primaryKey)) {
                return primaryValue;
            }
            ConcurrentHashMap<MetadataStrategy, T> secondaryMap = this.secondaryMap;
            if (secondaryMap != null) {
                T value = secondaryMap.get(strategy);
                if (value != null) {
                    return value;
                }
            }
        }
        return getSlow(strategy);
    }

    private synchronized T getSlow(MetadataStrategy strategy) {
        MetadataStrategy primaryKey = this.primaryKey;
        if (primaryKey == null) {
            T value = createValue(strategy);
            // Publish the value before the volatile key. A reader that sees
            // the key is therefore guaranteed to see the value as well.
            primaryValue = value;
            this.primaryKey = strategy;
            return value;
        }
        if (strategy.equals(primaryKey)) {
            return primaryValue;
        }
        ConcurrentHashMap<MetadataStrategy, T> secondaryMap = this.secondaryMap;
        if (secondaryMap == null) {
            this.secondaryMap = secondaryMap = new ConcurrentHashMap<>();
        } else {
            T value = secondaryMap.get(strategy);
            if (value != null) {
                return value;
            }
        }
        if (maxCount != -1 && secondaryMap.size() >= maxCount) {
            throw new IllegalStateException(
                    "Too many root sql clients are created, is it a bug?"
            );
        }
        T value = createValue(strategy);
        secondaryMap.put(strategy, value);
        return value;
    }

    private T createValue(MetadataStrategy strategy) {
        T value = creator.apply(strategy);
        if (value == null) {
            throw new AssertionError("Internal bug: creator of MetaCache cannot return null");
        }
        return value;
    }
}
