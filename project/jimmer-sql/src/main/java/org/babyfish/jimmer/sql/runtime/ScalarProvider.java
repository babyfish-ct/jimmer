package org.babyfish.jimmer.sql.runtime;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.Embeddable;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.MappedSuperclass;
import org.babyfish.jimmer.sql.ast.impl.TupleImplementor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.Consumer;

public abstract class ScalarProvider<T, S> {

    private final Type scalarType;

    private final Class<S> sqlType;

    public ScalarProvider(Class<T> scalarType, Class<S> sqlType) {
        validateScalarType(scalarType);
        this.scalarType = scalarType;
        this.sqlType = sqlType;
    }

    @SuppressWarnings("unchecked")
    protected ScalarProvider() {
        Map<TypeVariable<?>, Type> argMap = TypeUtils.getTypeArguments(this.getClass(), ScalarProvider.class);
        if (argMap.isEmpty()) {
            throw new IllegalStateException(
                    "Illegal type \"" +
                            getClass().getName() +
                            "\", it does not specify generic arguments for \"" +
                            ScalarProvider.class.getName() +
                            "\""
            );
        }
        TypeVariable<?>[] params = ScalarProvider.class.getTypeParameters();
        this.scalarType = (Type)argMap.get(params[0]);
        this.sqlType = (Class<S>) argMap.get(params[1]);
        validateScalarType(scalarType);
    }

    @NotNull
    public final Type getScalarType() {
        return scalarType;
    }

    @NotNull
    public final Class<S> getSqlType() {
        return sqlType;
    }

    @NotNull
    public abstract T toScalar(@NotNull S sqlValue) throws Exception;

    @NotNull
    public abstract S toSql(@NotNull T scalarValue) throws Exception;

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
     *     <li>Use {@link org.babyfish.jimmer.sql.JSqlClient.Builder#addScalarProvider(ImmutableProp, ScalarProvider)} or 
     *     {@link org.babyfish.jimmer.sql.JSqlClient.Builder#addScalarProvider(TypedProp, ScalarProvider)}</li>
     * </ul>
     * @return Null or handled property.
     */
    public Collection<ImmutableProp> getHandledProps() {
        return null;
    }

    public static <E extends Enum<E>> ScalarProvider<E, String> enumProviderByString(
            Class<E> enumType
    ) {
        return enumProviderByString(enumType, null);
    }

    public static <E extends Enum<E>> ScalarProvider<E, String> enumProviderByString(
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

    public static <E extends Enum<E>> ScalarProvider<E, Integer> enumProviderByInt(
            Class<E> enumType
    ) {
        return enumProviderByInt(enumType, null);
    }

    public static <E extends Enum<E>> ScalarProvider<E, Integer> enumProviderByInt(
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

    public final static ScalarProvider<UUID, byte[]> UUID_BY_BYTE_ARRAY =
            new ScalarProvider<UUID, byte[]>() {
                @Override
                public UUID toScalar(byte[] sqlValue) {
                    ByteBuffer byteBuffer = ByteBuffer.wrap(sqlValue);
                    long high = byteBuffer.getLong();
                    long low = byteBuffer.getLong();
                    return new UUID(high, low);
                }

                @Override
                public byte[] toSql(UUID scalarValue) {
                    ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[16]);
                    byteBuffer.putLong(scalarValue.getMostSignificantBits());
                    byteBuffer.putLong(scalarValue.getLeastSignificantBits());
                    return byteBuffer.array();
                }
            };

    public final static ScalarProvider<UUID, String> UUID_BY_STRING =
            new ScalarProvider<UUID, String>() {
                @Override
                public UUID toScalar(String sqlValue) {
                    return UUID.fromString(sqlValue);
                }

                @Override
                public String toSql(UUID scalarValue) {
                    return scalarValue.toString();
                }
            };

    private void validateScalarType(Type scalarType) {

        if (scalarType == UUID.class) {
            return; // UUID is standard type, but it can be overridden by ScalarProvider
        }

        if (!(scalarType instanceof Class<?>)) {
            return;
        }
        Class<?> scalarClass = (Class<?>) scalarType;

        if (scalarType == void.class) {
            throw new IllegalArgumentException(
                    "Illegal scalar type \"" +
                            scalarClass.getName() +
                            "\", it cannot be void"
            );
        }
        if (scalarType == Object.class) {
            throw new IllegalArgumentException(
                    "Illegal scalar type \"" +
                            scalarClass.getName() +
                            "\", scalar provider does not support object type which means any"
            );
        }
        if (TupleImplementor.class.isAssignableFrom(scalarClass)) {
            throw new IllegalArgumentException(
                    "Illegal scalar type \"" +
                            scalarClass.getName() +
                            "\", scalar provider does not support tuple type"
            );
        }
        if (ReaderManager.isStandardScalarType(scalarClass)) {
            throw new IllegalArgumentException(
                    "Illegal scalar type \"" +
                            ((Class<?>)scalarType).getName() +
                            "\", scalar provider does not support standard scalar type"
            );
        }
        Class<?> annotationType = getOrmAnnotationType(scalarClass);
        if (annotationType != null) {
            throw new IllegalArgumentException(
                    "Illegal scalar type \"" +
                            scalarClass.getName() +
                            "\", scalar provider does not support scalar type which is decorated by \"@" +
                            annotationType.getName() +
                            "\""
            );
        }
    }

    private Class<?> getOrmAnnotationType(Class<?> type) {
        if (type == null) {
            return null;
        }
        if (type != Object.class) {
            if (type.isAnnotationPresent(Entity.class)) {
                return Entity.class;
            }
            if (type.isAssignableFrom(MappedSuperclass.class)) {
                return MappedSuperclass.class;
            }
            if (type.isAssignableFrom(Embeddable.class)) {
                return Embeddable.class;
            }
        }
        Class<?> annoType = getOrmAnnotationType(type.getSuperclass());
        if (annoType != null) {
            return annoType;
        }
        for (Class<?> interfaceType : type.getInterfaces()) {
            annoType = getOrmAnnotationType(interfaceType);
            if (annoType != null) {
                return annoType;
            }
        }
        return null;
    }
}
