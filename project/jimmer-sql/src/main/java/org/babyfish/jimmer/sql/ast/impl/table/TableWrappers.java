package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.sql.ast.table.TableEx;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.spi.AbstractTableWrapper;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class TableWrappers {

    private static final Map<Class<?>, Constructor<?>> positiveCacheMap =
            new WeakHashMap<>();

    private static final Map<Class<?>, Void> negativeCacheMap =
            new LRUMap<>();

    private static final ReadWriteLock cacheLock =
            new ReentrantReadWriteLock();

    private TableWrappers() {}

    @SuppressWarnings("unchecked")
    public static <T extends Table<?>> T wrap(TableImplementor<?> table) {
        if (table instanceof TableEx<?>) {
            return TableExWrappers.wrap(table);
        }
        Class<?> javaClass = table.getImmutableType().getJavaClass();
        Constructor<?> constructor = tryGetConstructor(javaClass);
        if (constructor == null) {
            throw new IllegalStateException(
                    "No Table wrapper class for \"" + table.getImmutableType() +"\""
            );
        }
        try {
            return (T) constructor.newInstance(table);
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

        Constructor<?> constuctor;
        Lock lock;

        (lock = cacheLock.readLock()).lock();
        try {
            if (negativeCacheMap.containsKey(javaClass)) {
                return null;
            }
            constuctor = positiveCacheMap.get(javaClass);
        } finally {
            lock.unlock();
        }

        if (constuctor == null) {
            (lock = cacheLock.writeLock()).lock();
            try {
                if (negativeCacheMap.containsKey(javaClass)) {
                    return null;
                }
                constuctor = positiveCacheMap.get(javaClass);
                if (constuctor == null) {
                    constuctor = createConstructor(javaClass);
                    if (constuctor != null) {
                        positiveCacheMap.put(javaClass, constuctor);
                    } else {
                        negativeCacheMap.put(javaClass, null);
                    }
                }
            } finally {
                lock.unlock();
            }
        }
        return constuctor;
    }

    private static Constructor<?> createConstructor(Class<?> javaClass) {
        Class<?> tableClass;
        try {
            tableClass = Class.forName(javaClass.getName() + "Table");
        } catch (ClassNotFoundException ex) {
            return null;
        }
        if (!AbstractTableWrapper.class.isAssignableFrom(tableClass)) {
            return null;
        }
        Constructor<?> constructor;
        try {
            constructor = tableClass.getConstructor(Table.class);
        } catch (NoSuchMethodException ex) {
            return null;
        }
        return constructor;
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
