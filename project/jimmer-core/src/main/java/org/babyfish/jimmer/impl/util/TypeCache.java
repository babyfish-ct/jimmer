package org.babyfish.jimmer.impl.util;

import org.babyfish.jimmer.meta.ImmutableType;

import java.util.function.Function;

public class TypeCache<V> {

    private final Function<ImmutableType, V> creator;

    private final boolean nullable;

    private final ClassCache<Entry<V>> classCache;

    private final StaticCache<ImmutableType, V> fallbackCache;

    public TypeCache(Function<ImmutableType, V> creator) {
        this(creator, false);
    }

    public TypeCache(Function<ImmutableType, V> creator, boolean nullable) {
        this.creator = creator;
        this.nullable = nullable;
        classCache = new ClassCache<>(this::createEntry);
        fallbackCache = new StaticCache<>(creator, nullable);
    }

    public V get(ImmutableType key) {
        Entry<V> entry = classCache.get(key.getJavaClass());
        if (entry.type == key) {
            return entry.value;
        }
        return fallbackCache.get(key);
    }

    private Entry<V> createEntry(Class<?> javaClass) {
        ImmutableType type = ImmutableType.tryGet(javaClass);
        if (type == null) {
            return new Entry<>(null, null);
        }
        V value = creator.apply(type);
        if (value == null && !nullable) {
            throw new IllegalStateException(
                    "The creator cannot return null because current type cache does not accept null values"
            );
        }
        return new Entry<>(type, value);
    }

    private static class Entry<V> {

        final ImmutableType type;

        final V value;

        private Entry(ImmutableType type, V value) {
            this.type = type;
            this.value = value;
        }
    }
}
