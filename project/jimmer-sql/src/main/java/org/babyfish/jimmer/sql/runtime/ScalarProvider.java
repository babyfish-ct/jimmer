package org.babyfish.jimmer.sql.runtime;

import org.apache.commons.lang3.reflect.TypeUtils;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public abstract class ScalarProvider<T, S> {

    private final Class<T> scalarType;

    private final Class<S> sqlType;

    public ScalarProvider(Class<T> scalarType, Class<S> sqlType) {
        this.scalarType = scalarType;
        this.sqlType = sqlType;
    }

    @SuppressWarnings("unchecked")
    protected ScalarProvider() {
        Map<TypeVariable<?>, Type> argMap = TypeUtils.getTypeArguments(this.getClass(), ScalarProvider.class);
        TypeVariable<?>[] params = ScalarProvider.class.getTypeParameters();
        this.scalarType = (Class<T>)argMap.get(params[0]);
        this.sqlType = (Class<S>) argMap.get(params[1]);
    }

    public final Class<T> getScalarType() {
        return scalarType;
    }

    public final Class<S> getSqlType() {
        return sqlType;
    }

    public abstract T toScalar(S sqlValue);

    public abstract S toSql(T scalarValue);

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
}
