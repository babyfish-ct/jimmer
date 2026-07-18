package org.babyfish.jimmer.impl.util;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;

import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Function;

public class PropCache<V> {

    private static final Object NULL = new Object();

    private final Function<ImmutableProp, V> creator;

    private final boolean nullable;

    private final ClassCache<Bucket<V>> classCache;

    private final StaticCache<ImmutableProp, V> fallbackCache;

    public PropCache(Function<ImmutableProp, V> creator) {
        this(creator, false);
    }

    public PropCache(Function<ImmutableProp, V> creator, boolean nullable) {
        this.creator = creator;
        this.nullable = nullable;
        classCache = new ClassCache<>(this::createBucket);
        fallbackCache = new StaticCache<>(creator, nullable);
    }

    public V get(ImmutableProp key) {
        ImmutableType declaringType = key.getDeclaringType();
        Bucket<V> bucket = classCache.get(declaringType.getJavaClass());
        int index = key.getId().asIndex();
        if (bucket.type == declaringType && index >= 0 && index < bucket.values.length()) {
            return bucket.get(key, index);
        }
        return fallbackCache.get(key);
    }

    private Bucket<V> createBucket(Class<?> javaClass) {
        ImmutableType type = ImmutableType.tryGet(javaClass);
        if (type == null) {
            return new Bucket<>(null, 0, creator, nullable);
        }
        int size = 0;
        for (ImmutableProp prop : type.getProps().values()) {
            int index = prop.getId().asIndex();
            if (index >= size) {
                size = index + 1;
            }
        }
        return new Bucket<>(type, size, creator, nullable);
    }

    private static class Bucket<V> {

        final ImmutableType type;

        final AtomicReferenceArray<Object> values;

        private final Function<ImmutableProp, V> creator;

        private final boolean nullable;

        private Bucket(
                ImmutableType type,
                int size,
                Function<ImmutableProp, V> creator,
                boolean nullable
        ) {
            this.type = type;
            this.values = new AtomicReferenceArray<>(size);
            this.creator = creator;
            this.nullable = nullable;
        }

        @SuppressWarnings("unchecked")
        V get(ImmutableProp prop, int index) {
            Object value = values.get(index);
            if (value == null) {
                V created = creator.apply(prop);
                if (created == null && !nullable) {
                    throw new IllegalStateException(
                            "The creator cannot return null because current prop cache does not accept null values"
                    );
                }
                Object newValue = created != null ? created : NULL;
                if (values.compareAndSet(index, null, newValue)) {
                    value = newValue;
                } else {
                    value = values.get(index);
                }
            }
            return value != NULL ? (V) value : null;
        }
    }
}
