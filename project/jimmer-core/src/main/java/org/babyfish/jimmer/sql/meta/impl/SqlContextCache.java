package org.babyfish.jimmer.sql.meta.impl;

import org.babyfish.jimmer.sql.meta.SqlContext;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Function;

/*
 * Often there is only ONE key,
 * so keep it outside the lazily created WeakHashMap.
 */
public class SqlContextCache<T> {

    private final Function<SqlContext, T> creator;

    private volatile SqlContext primaryKey;

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

        SqlContext primaryKey = this.primaryKey;
        if (strategy.equals(primaryKey)) {
            return primaryValue;
        }
        return getSlow(strategy);
    }

    private synchronized T getSlow(SqlContext strategy) {
        SqlContext primaryKey = this.primaryKey;
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
        Map<SqlContext, T> otherMap = this.otherMap;
        if (otherMap != null && otherMap.size() > 512) {
            this.otherMap = otherMap = null;
        }
        if (otherMap != null) {
            T value = otherMap.get(strategy);
            if (value != null) {
                return value;
            }
        }
        T value = createValue(strategy);
        if (otherMap == null) {
            this.otherMap = otherMap = new WeakHashMap<>();
        }
        otherMap.put(strategy, value);
        return value;
    }

    private T createValue(SqlContext strategy) {
        T value = creator.apply(strategy);
        if (value == null) {
            throw new AssertionError("Internal bug: creator of MetaCache cannot return null");
        }
        return value;
    }
}
