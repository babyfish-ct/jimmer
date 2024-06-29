package org.babyfish.jimmer.sql.fetcher;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.babyfish.jimmer.Dto;
import org.babyfish.jimmer.impl.util.ClassCache;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public final class DtoMetadata<E, V> {

    private static final ClassCache<DtoMetadata<?, ?>> cache =
            new ClassCache<>(DtoMetadata::create, false);

    private final Fetcher<E> fetcher;

    private final Function<E, V> converter;

    /**
     * This constructor should not be invoked by developer,
     * it is designed for code generator.
     * @param fetcher
     * @param converter
     */
    public DtoMetadata(Fetcher<E> fetcher, Function<E, V> converter) {
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
        DtoMetadata<?, ?> that = (DtoMetadata<?, ?>) o;
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
    public static <E, V extends Dto<E>> DtoMetadata<E, V> of(Class<V> dtoType) {
        return (DtoMetadata<E, V>) cache.get(dtoType);
    }

    private static DtoMetadata<?, ?> create(Class<?> dtoType) {
        if (!Dto.class.isAssignableFrom(dtoType)) {
            throw new IllegalArgumentException(
                    "The type \"" +
                            dtoType.getName() +
                            "\" does not inherit \"" +
                            Dto.class.getName() +
                            "\""
            );
        }
        Iterator<Type> itr = TypeUtils.getTypeArguments(dtoType, Dto.class).values().iterator();
        if (!itr.hasNext()) {
            throw new IllegalArgumentException(
                    "The type \"" +
                            dtoType.getName() +
                            "\" does not specify the generic parameter of \"" +
                            Dto.class.getName() +
                            "\""
            );
        }
        Type type = itr.next();
        if (!(type instanceof Class<?>) || !((Class<?>)type).isInterface()) {
            throw new IllegalArgumentException(
                    "The type \"" +
                            dtoType.getName() +
                            "\" is illegal, the generic parameter of \"" +
                            Dto.class.getName() +
                            "\" must be specified"
            );
        }
        Class<?> entityType = (Class<?>)type;
        Field metadataField;
        try {
            metadataField = dtoType.getDeclaredField("METADATA");
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
                            dtoType.getName() +
                            "\" is illegal, there is not static final field \"METADATA\""
            );
        }
        if (metadataField.getType() != DtoMetadata.class) {
            throw new IllegalArgumentException(
                    "The type \"" +
                            dtoType.getName() +
                            "\" is illegal, the type of \"" +
                            metadataField +
                            "\" must be \"" +
                            DtoMetadata.class.getName() +
                            "\""
            );
        }
        TypeVariable<?>[] typeParameters = DtoMetadata.class.getTypeParameters();
        Map<TypeVariable<?>, Type> typeArgumentMap =
                TypeUtils.getTypeArguments(metadataField.getGenericType(), DtoMetadata.class);
        if (typeArgumentMap.get(typeParameters[0]) != entityType) {
            throw new IllegalArgumentException(
                    "The type \"" +
                            dtoType.getName() +
                            "\" is illegal, the first generic argument of the return type of \"" +
                            metadataField +
                            "\" must be \"" +
                            entityType.getName() +
                            "\""
            );
        }
        if (typeArgumentMap.get(typeParameters[1]) != dtoType) {
            throw new IllegalArgumentException(
                    "The type \"" +
                            dtoType.getName() +
                            "\" is illegal, the first generic argument of the return type of \"" +
                            metadataField +
                            "\" must be \"" +
                            dtoType.getName() +
                            "\""
            );
        }
        metadataField.setAccessible(true);
        try {
            return (DtoMetadata<?, ?>)metadataField.get(null);
        } catch (IllegalAccessException ex) {
            throw new AssertionError("Internal bug", ex);
        }
    }
}
