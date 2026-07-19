package org.babyfish.jimmer.sql.fetcher;

import org.babyfish.jimmer.Dto;
import org.babyfish.jimmer.lang.Generics;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.function.Function;

public final class DtoMetadata<E, V> {

    private static final ClassValue<DtoMetadata<?, ?>> CACHE = new ClassValue<DtoMetadata<?, ?>>() {
        @Override
        protected DtoMetadata<?, ?> computeValue(@NotNull Class<?> type) {
            return create(type);
        }
    };

    @Nullable
    private final Class<V> dtoType;

    private final Fetcher<E> fetcher;

    private final Function<E, V> converter;

    /**
     * This constructor should not be invoked by developer,
     * it is designed for code generator.
     * @param fetcher
     * @param converter
     */
    public DtoMetadata(@NotNull Fetcher<E> fetcher, @NotNull Function<E, V> converter) {
        this.dtoType = null;
        this.fetcher = fetcher;
        this.converter = converter;
    }

    /**
     * This constructor should not be invoked by developer,
     * it is designed for code generator.
     * @param dtoType
     * @param fetcher
     * @param converter
     */
    public DtoMetadata(
            @NotNull Class<V> dtoType,
            @NotNull Fetcher<E> fetcher,
            @NotNull Function<E, V> converter
    ) {
        this.dtoType = dtoType;
        this.fetcher = fetcher;
        this.converter = converter;
    }

    @Nullable
    public Class<V> getDtoType() {
        return dtoType;
    }

    @NotNull
    public Fetcher<E> getFetcher() {
        return fetcher;
    }

    @NotNull
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
    @NotNull
    public static <E, V extends Dto<E>> DtoMetadata<E, V> of(@NotNull Class<V> dtoType) {
        return (DtoMetadata<E, V>) CACHE.get(dtoType);
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
        Type[] types = Generics.getTypeArguments(dtoType, Dto.class);
        if (types.length == 0) {
            throw new IllegalArgumentException(
                    "The type \"" +
                            dtoType.getName() +
                            "\" does not specify the generic parameter of \"" +
                            Dto.class.getName() +
                            "\""
            );
        }
        Type type = types[0];
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
        Type[] typeArguments =
                Generics.getTypeArguments(metadataField.getGenericType(), DtoMetadata.class);
        if (typeArguments[0] != entityType) {
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
        if (typeArguments[1] != dtoType) {
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
            DtoMetadata<?, ?> metadata = (DtoMetadata<?, ?>) metadataField.get(null);
            return metadata.dtoType != null ?
                    metadata :
                    metadata.withDtoType(dtoType);
        } catch (IllegalAccessException ex) {
            throw new AssertionError("Internal bug", ex);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private DtoMetadata<?, ?> withDtoType(Class<?> dtoType) {
        return new DtoMetadata(dtoType, fetcher, converter);
    }
}
