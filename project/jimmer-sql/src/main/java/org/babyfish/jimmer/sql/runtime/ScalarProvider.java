package org.babyfish.jimmer.sql.runtime;

import java.util.function.Consumer;

public abstract class ScalarProvider<T, S> {

    private Class<T> scalarType;

    private Class<S> sqlType;

    protected ScalarProvider(Class<T> scalarType, Class<S> sqlType) {
        this.scalarType = scalarType;
        this.sqlType = sqlType;
    }

    public Class<T> getScalarType() {
        return scalarType;
    }

    public Class<S> getSqlType() {
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
}
