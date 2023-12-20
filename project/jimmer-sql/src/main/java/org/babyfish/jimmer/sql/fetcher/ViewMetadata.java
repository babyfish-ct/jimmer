package org.babyfish.jimmer.sql.fetcher;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.babyfish.jimmer.View;
import org.babyfish.jimmer.impl.util.ClassCache;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public final class ViewMetadata<E, V extends View<E>> {

    private static final ClassCache<ViewMetadata<?, ?>> cache =
            new ClassCache<>(ViewMetadata::create, false);

    private final Fetcher<E> fetcher;

    private final Function<E, V> converter;

    /**
     * This constructor should not be invoked by developer,
     * it is designed for code generator.
     * @param fetcher
     * @param converter
     */
    public ViewMetadata(Fetcher<E> fetcher, Function<E, V> converter) {
        this.fetcher = Objects.requireNonNull(fetcher, "fetch cannot be null");
        this.converter = Objects.requireNonNull(converter, "converter cannot be null");
    }

    public Fetcher<E> getFetcher() {
        return fetcher;
    }

    public Function<E, V> getConverter() {
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
        ViewMetadata<?, ?> that = (ViewMetadata<?, ?>) o;
        return fetcher.equals(that.fetcher) && converter.equals(that.converter);
    }

    @Override
    public String toString() {
        return "ViewMetadata{" +
                "fetcher=" + fetcher +
                ", converter=" + converter +
                '}';
    }

    @SuppressWarnings("unchecked")
    public static <E, V extends View<E>> ViewMetadata<E, V> of(Class<V> viewType) {
        return (ViewMetadata<E, V>) cache.get(viewType);
    }

    private static ViewMetadata<?, ?> create(Class<?> viewType) {
        if (!View.class.isAssignableFrom(viewType)) {
            throw new IllegalArgumentException(
                    "The type \"" +
                            viewType.getName() +
                            "\" does not inherit \"" +
                            View.class.getName() +
                            "\""
            );
        }
        Iterator<Type> itr = TypeUtils.getTypeArguments(viewType, View.class).values().iterator();
        if (!itr.hasNext()) {
            throw new IllegalArgumentException(
                    "The type \"" +
                            viewType.getName() +
                            "\" does not specify the generic parameter of \"" +
                            View.class.getName() +
                            "\""
            );
        }
        Type type = itr.next();
        if (!(type instanceof Class<?>) || !((Class<?>)type).isInterface()) {
            throw new IllegalArgumentException(
                    "The type \"" +
                            viewType.getName() +
                            "\"illegal, the generic parameter of \"" +
                            View.class.getName() +
                            "\" must be interface"
            );
        }
        Class<?> entityType = (Class<?>)type;
        Field metadataField;
        try {
            metadataField = viewType.getDeclaredField("METADATA");
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
                            viewType.getName() +
                            "\" is illegal, there is not static final field \"METADATA\""
            );
        }
        if (metadataField.getType() != ViewMetadata.class) {
            throw new IllegalArgumentException(
                    "The type \"" +
                            viewType.getName() +
                            "\" is illegal, the type of \"" +
                            metadataField +
                            "\" must be \"" +
                            ViewMetadata.class.getName() +
                            "\""
            );
        }
        TypeVariable<?>[] typeParameters = ViewMetadata.class.getTypeParameters();
        Map<TypeVariable<?>, Type> typeArgumentMap =
                TypeUtils.getTypeArguments(metadataField.getGenericType(), ViewMetadata.class);
        if (typeArgumentMap.get(typeParameters[0]) != entityType) {
            throw new IllegalArgumentException(
                    "The type \"" +
                            viewType.getName() +
                            "\" is illegal, the first generic argument of the return type of \"" +
                            metadataField +
                            "\" must be \"" +
                            entityType.getName() +
                            "\""
            );
        }
        if (typeArgumentMap.get(typeParameters[1]) != viewType) {
            throw new IllegalArgumentException(
                    "The type \"" +
                            viewType.getName() +
                            "\" is illegal, the first generic argument of the return type of \"" +
                            metadataField +
                            "\" must be \"" +
                            viewType.getName() +
                            "\""
            );
        }
        metadataField.setAccessible(true);
        try {
            return (ViewMetadata<?, ?>)metadataField.get(null);
        } catch (IllegalAccessException ex) {
            throw new AssertionError("Internal bug", ex);
        }
    }
}
