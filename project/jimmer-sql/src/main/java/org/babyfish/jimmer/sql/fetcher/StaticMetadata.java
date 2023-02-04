package org.babyfish.jimmer.sql.fetcher;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.babyfish.jimmer.Static;
import org.babyfish.jimmer.impl.util.StaticCache;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public final class StaticMetadata<E, S> {

    private static final StaticCache<Class<Static<?>>, StaticMetadata<?, ?>> cache =
            new StaticCache<>(StaticMetadata::create, false);

    private final Fetcher<E> fetcher;

    private final Function<E, S> converter;

    public StaticMetadata(Fetcher<E> fetcher, Function<E, S> converter) {
        this.fetcher = Objects.requireNonNull(fetcher, "fetch cannot be null");
        this.converter = Objects.requireNonNull(converter, "converter cannot be null");
    }

    public Fetcher<E> getFetcher() {
        return fetcher;
    }

    public Function<E, S> getConverter() {
        return converter;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fetcher, converter);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StaticMetadata<?, ?> that = (StaticMetadata<?, ?>) o;
        return fetcher.equals(that.fetcher) && converter.equals(that.converter);
    }

    @Override
    public String toString() {
        return "StaticMetadata{" +
                "fetcher=" + fetcher +
                ", converter=" + converter +
                '}';
    }

    @SuppressWarnings("unchecked")
    public static <E, S extends Static<E>> StaticMetadata<E, S> of(Class<S> staticType) {
        return (StaticMetadata<E, S>) cache.get((Class<Static<?>>)staticType);
    }

    public static StaticMetadata<?, ?> create(Class<Static<?>> staticType) {
        if (!Static.class.isAssignableFrom(staticType)) {
            throw new IllegalArgumentException(
                    "The type \"" +
                            staticType.getName() +
                            "\" does not inherit \"" +
                            Static.class.getName() +
                            "\""
            );
        }
        Iterator<Type> itr = TypeUtils.getTypeArguments(staticType, Static.class).values().iterator();
        if (!itr.hasNext()) {
            throw new IllegalArgumentException(
                    "The type \"" +
                            staticType.getName() +
                            "\" does not specify the generic parameter of \"" +
                            Static.class.getName() +
                            "\""
            );
        }
        Type type = itr.next();
        if (!(type instanceof Class<?>) || !((Class<?>)type).isInterface()) {
            throw new IllegalArgumentException(
                    "The type \"" +
                            staticType.getName() +
                            "\"illegal, the generic parameter of \"" +
                            Static.class.getName() +
                            "\" must be interface"
            );
        }
        Class<?> entityType = (Class<?>)type;
        Field metadataField;
        try {
            metadataField = staticType.getDeclaredField("METADATA");
            if (!Modifier.isStatic(metadataField.getModifiers()) ||
                    !Modifier.isFinal(metadataField.getModifiers())) {
                metadataField = null;
            }
        } catch (NoSuchFieldException ex) {
            metadataField = null;
        }
        if (metadataField == null) {
            throw new IllegalArgumentException(
                    "The type \"" +
                            staticType.getName() +
                            "\" is illegal, there is not static final field \"METADATA\""
            );
        }
        if (metadataField.getType() != StaticMetadata.class) {
            throw new IllegalArgumentException(
                    "The type \"" +
                            staticType.getName() +
                            "\" is illegal, the type of \"" +
                            metadataField +
                            "\" must be \"" +
                            StaticMetadata.class.getName() +
                            "\""
            );
        }
        TypeVariable<?>[] typeParameters = StaticMetadata.class.getTypeParameters();
        Map<TypeVariable<?>, Type> typeArgumentMap =
                TypeUtils.getTypeArguments(metadataField.getGenericType(), StaticMetadata.class);
        if (typeArgumentMap.get(typeParameters[0]) != entityType) {
            throw new IllegalArgumentException(
                    "The type \"" +
                            staticType.getName() +
                            "\" is illegal, the first generic argument of the return type of \"" +
                            metadataField +
                            "\" must be \"" +
                            entityType.getName() +
                            "\""
            );
        }
        if (typeArgumentMap.get(typeParameters[1]) != staticType) {
            throw new IllegalArgumentException(
                    "The type \"" +
                            staticType.getName() +
                            "\" is illegal, the first generic argument of the return type of \"" +
                            metadataField +
                            "\" must be \"" +
                            staticType.getName() +
                            "\""
            );
        }
        metadataField.setAccessible(true);
        try {
            return (StaticMetadata<?, ?>)metadataField.get(null);
        } catch (IllegalAccessException ex) {
            throw new AssertionError("Internal bug", ex);
        }
    }
}
