package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.table.SubQueryTable;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.spi.AbstractTableWrapper;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

class SubQueryTableWrappers {

    private static final Map<Class<?>, Constructor<?>> positiveCacheMap =
            new WeakHashMap<>();

    private static final Map<Class<?>, Void> negativeCacheMap =
            new LRUMap<>();

    private static final ReadWriteLock cacheLock =
            new ReentrantReadWriteLock();

    private SubQueryTableWrappers() {}

    public static Table<?> wrap(TableImpl<?> table) {
        Class<?> javaClass = table.getImmutableType().getJavaClass();
        Constructor<?> constructor = tryGetConstructor(javaClass);
        if (constructor == null) {
            return table;
        }
        try {
            return (Table<?>) constructor.newInstance(table);
        } catch (InstantiationException | IllegalAccessException ex) {
            throw new AssertionError(
                    "Internal bug: Can not create instance of " +
                            constructor.getDeclaringClass().getName()
            );
        } catch (InvocationTargetException ex) {
            Throwable target = ex.getTargetException();
            if (target instanceof RuntimeException) {
                throw (RuntimeException)target;
            }
            if (target instanceof Error) {
                throw (Error)target;
            }
            throw new AssertionError(
                    "Internal bug: Can not create instance of " +
                            constructor.getDeclaringClass().getName()
            );
        }
    }

    private static Constructor<?> tryGetConstructor(Class<?> javaClass) {

        Constructor<?> constructor;
        Lock lock;

        (lock = cacheLock.readLock()).lock();
        try {
            if (negativeCacheMap.containsKey(javaClass)) {
                return null;
            }
            constructor = positiveCacheMap.get(javaClass);
        } finally {
            lock.unlock();
        }

        if (constructor == null) {
            (lock = cacheLock.writeLock()).lock();
            try {
                if (negativeCacheMap.containsKey(javaClass)) {
                    return null;
                }
                constructor = positiveCacheMap.get(javaClass);
                if (constructor == null) {
                    constructor = createWrapperClass(javaClass);
                    if (constructor != null) {
                        positiveCacheMap.put(javaClass, constructor);
                    } else {
                        negativeCacheMap.put(javaClass, null);
                    }
                }
            } finally {
                lock.unlock();
            }
        }
        return constructor;
    }

    private static Constructor<?> createWrapperClass(Class<?> javaClass) {
        Class<?> tableClass;
        try {
            tableClass = Class.forName(javaClass.getName() + "Table");
        } catch (ClassNotFoundException ex) {
            return null;
        }
        if (!AbstractTableWrapper.class.isAssignableFrom(tableClass)) {
            return null;
        }
        Class<?> sqtClass = Arrays.stream(tableClass.getClasses())
                .filter(it -> it.getSimpleName().equals("SQT"))
                .findFirst()
                .orElse(null);
        if (sqtClass == null ||
                !Modifier.isStatic(sqtClass.getModifiers()) ||
                !AbstractTableWrapper.class.isAssignableFrom(sqtClass)) {
            return null;
        }
        try {
            return sqtClass.getConstructor(SubQueryTable.class);
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    private static class LRUMap<K, V> extends LinkedHashMap<K, V> {

        LRUMap() {
            super(200, .75F, true);
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry eldest) {
            return true;
        }
    }
}
