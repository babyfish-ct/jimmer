package org.babyfish.jimmer.meta.impl;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.babyfish.jimmer.Draft;
import org.babyfish.jimmer.Immutable;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.runtime.DraftContext;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiFunction;

public class Metadata {

    private Metadata() {}

    private static final Class<?> TABLE_CLASS;

    private static Map<Class<?>, ImmutableTypeImpl> positiveCacheMap =
            new WeakHashMap<Class<?>, ImmutableTypeImpl>();

    private static Map<Class<?>, Void> negativeCacheMap =
            new LRUMap<>();

    private static ReadWriteLock cacheLock =
            new ReentrantReadWriteLock();

    public static ImmutableTypeImpl get(Class<?> javaClass) {
        ImmutableTypeImpl immutableType = tryGet(javaClass);
        if (immutableType == null) {
            throw new IllegalArgumentException(
                    "Cannot get immutable type for \"" + javaClass.getName() + "\""
            );
        }
        return immutableType;
    }

    public static ImmutableTypeImpl tryGet(Class<?> javaClass) {

        ImmutableTypeImpl immutableType;
        Lock lock;

        (lock = cacheLock.readLock()).lock();
        try {
            if (negativeCacheMap.containsKey(javaClass)) {
                return null;
            }
            immutableType = positiveCacheMap.get(javaClass);
        } finally {
            lock.unlock();
        }

        if (immutableType == null) {
            (lock = cacheLock.writeLock()).lock();
            try {
                if (negativeCacheMap.containsKey(javaClass)) {
                    return null;
                }
                immutableType = positiveCacheMap.get(javaClass);
                if (immutableType == null) {
                    immutableType = create(javaClass);
                    if (immutableType != null) {
                        positiveCacheMap.put(javaClass, immutableType);
                    } else {
                        negativeCacheMap.put(javaClass, null);
                    }
                }
            } finally {
                lock.unlock();
            }
        }
        return immutableType;
    }

    private static ImmutableTypeImpl create(Class<?> javaClass) {
        if (TABLE_CLASS != null && TABLE_CLASS.isAssignableFrom(javaClass)) {
            if (javaClass.getTypeParameters().length != 0) {
                return null;
            }
            Type type = TypeUtils
                    .getTypeArguments(javaClass, TABLE_CLASS)
                    .values()
                    .iterator()
                    .next();
            if (!(type instanceof Class<?>)) {
                return null;
            }
            javaClass = (Class<?>) type;
        }
        Class<?> immutableJavaClass = getImmutableJavaClass(javaClass);
        if (immutableJavaClass == null) {
            return null;
        }
        Class<?> draftClass;
        try {
            draftClass = Class.forName(immutableJavaClass.getName() + "Draft");
        } catch (ClassNotFoundException ex) {
            throw new IllegalArgumentException(
                    "Cannot find draft type for \"" + immutableJavaClass.getName() + "\""
            );
        }
        Class<?> producerClass = Arrays
                .stream(draftClass.getDeclaredClasses())
                .filter(it -> it.getSimpleName().equals("Producer"))
                .findFirst()
                .orElse(null);
        if (producerClass == null) {
            throw new IllegalArgumentException(
                    "Cannot find producer type for \"" + draftClass.getName() + "\""
            );
        }
        Field typeField;
        try {
            typeField = producerClass.getField("TYPE");
        } catch (NoSuchFieldException ex) {
            typeField = null;
        }
        if (typeField == null ||
                !Modifier.isPublic(typeField.getModifiers()) ||
                !Modifier.isStatic(typeField.getModifiers()) ||
                !Modifier.isFinal(typeField.getModifiers()) ||
                typeField.getType() != ImmutableType.class
        ) {
            throw new IllegalArgumentException(
                    "Illegal producer type \"" + producerClass.getName() + "\""
            );
        }
        try {
            return (ImmutableTypeImpl) typeField.get(null);
        } catch (IllegalAccessException e) {
            throw new AssertionError("Internal bug: Cannot access " + typeField);
        }
    }

    public static ImmutableType.Builder newTypeBuilder(
            Class<?> javaClass,
            ImmutableType superType,
            BiFunction<DraftContext, Object, Draft> draftFactory
    ) {
        return new ImmutableTypeImpl.BuilderImpl(javaClass, superType, draftFactory);
    }

    private static Class<?> getImmutableJavaClass(Class<?> javaClass) {
        boolean matched = Arrays.stream(javaClass.getAnnotations()).anyMatch(
                it -> it.annotationType() == Immutable.class ||
                        it.annotationType().getName().equals("javax.persistence.Entity")
        );
        if (matched) {
            return javaClass;
        }
        Class<?> superClass = javaClass.getSuperclass();
        if (superClass != null && superClass != Object.class) {
            Class<?> immutableJavaClass = getImmutableJavaClass(superClass);
            if (immutableJavaClass != null) {
                return immutableJavaClass;
            }
        }
        for (Class<?> interfaceClass : javaClass.getInterfaces()) {
            Class<?> immutableJavaClass = getImmutableJavaClass(interfaceClass);
            if (immutableJavaClass != null) {
                return immutableJavaClass;
            }
        }
        return null;
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

    static {
        Class<?> tableClass;
        try {
            tableClass = Class.forName("org.babyfish.jimmer.sql.ast.table.Table");
        } catch (ClassNotFoundException ex) {
            tableClass = null;
        }
        TABLE_CLASS = tableClass;
    }
}
