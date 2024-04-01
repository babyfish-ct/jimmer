package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.TypedProp;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Consumer;

public interface ScalarProvider<T, S> {

    @NotNull
    default Type getScalarType() {
        return AbstractScalarProvider.META_CACHE.get(this.getClass()).scalarType;
    }

    @SuppressWarnings("unchecked")
    @NotNull
    default Class<S> getSqlType() {
        return (Class<S>) AbstractScalarProvider.META_CACHE.get(this.getClass()).sqlType;
    }

    T toScalar(@NotNull S sqlValue) throws Exception;

    S toSql(@NotNull T scalarValue) throws Exception;

    /**
     * User can override this method, it can return null, empty or handled property.
     * <ul>
     *     <li>Null or empty: Global scalar provider, can be applied to any properties</li>
     *     <li>Otherwise: Property-specific scalar provider</li>
     * </ul>
     *
     * <p>Actually, there are two ways to add property-specific scalar providers</p>
     * <ul>
     *     <li>Override {@link #getHandledProps()}</li>
     *     <li>Use {@link org.babyfish.jimmer.sql.JSqlClient.Builder#setScalarProvider(ImmutableProp, ScalarProvider)} or
     *     {@link org.babyfish.jimmer.sql.JSqlClient.Builder#setScalarProvider(TypedProp, ScalarProvider)}</li>
     * </ul>
     * @return Null or handled property.
     */
    default Collection<ImmutableProp> getHandledProps() {
        return null;
    }

    default boolean isJsonScalar() {
        return false;
    }

    default Reader<S> reader() {
        return null;
    }

    static <E extends Enum<E>> ScalarProvider<E, String> enumProviderByString(
            Class<E> enumType
    ) {
        return enumProviderByString(enumType, null);
    }

    static <E extends Enum<E>> ScalarProvider<E, String> enumProviderByString(
            Class<E> enumType,
            Consumer<EnumProviderBuilder<E, String>> block
    ) {
        EnumProviderBuilder<E, String> builder =
                EnumProviderBuilder.of(enumType, String.class, Enum::name);
        if (block != null) {
            block.accept(builder);
        }
        return builder.build();
    }

    static <E extends Enum<E>> ScalarProvider<E, Integer> enumProviderByInt(
            Class<E> enumType
    ) {
        return enumProviderByInt(enumType, null);
    }

    static <E extends Enum<E>> ScalarProvider<E, Integer> enumProviderByInt(
            Class<E> enumType,
            Consumer<EnumProviderBuilder<E, Integer>> block
    ) {
        EnumProviderBuilder<E, Integer> builder =
                EnumProviderBuilder.of(enumType, Integer.class, Enum::ordinal);
        if (block != null) {
            block.accept(builder);
        }
        return builder.build();
    }

    static ScalarProvider<UUID, byte[]> uuidByByteArray() {
        return AbstractScalarProvider.UUID_BY_BYTE_ARRAY;
    }

    static ScalarProvider<UUID, String> uuidByString() {
        return AbstractScalarProvider.UUID_BY_STRING;
    }
}
