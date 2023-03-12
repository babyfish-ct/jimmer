package org.babyfish.jimmer.sql.runtime;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class EnumProviderBuilder<E extends Enum<E>, S> {

    private final Class<E> enumType;

    private final Class<S> sqlType;

    private final Function<E, S> defaultSqlValueMapper;

    private final Map<E, S> sqlMap = new HashMap<>();

    public static <E extends Enum<E>, S> EnumProviderBuilder<E, S> of(
            Class<E> enumType,
            Class<S> sqlType,
            Function<E, S> defaultSqlValueMapper
    ) {
        return new EnumProviderBuilder<>(enumType, sqlType, defaultSqlValueMapper);
    }

    private EnumProviderBuilder(
            Class<E> enumType,
            Class<S> sqlType,
            Function<E, S> defaultSqlValueMapper
    ) {
        this.enumType = enumType;
        this.sqlType = sqlType;
        this.defaultSqlValueMapper = defaultSqlValueMapper;
    }

    public EnumProviderBuilder<E, S> map(@NotNull E enumValue, @NotNull S sqlValue) {
        if (sqlMap.containsValue(enumValue)) {
            throw new IllegalStateException("'${enumValue}' has been mapped");
        }
        sqlMap.put(enumValue, sqlValue);
        return this;
    }

    public EnumProvider<E, S> build() {
        Map<E, S> sqlMap = new HashMap<>(this.sqlMap);
        for (E enumValue : enumType.getEnumConstants()) {
            sqlMap.computeIfAbsent(enumValue, defaultSqlValueMapper);
        }
        Map<S, E> enumMap = new HashMap<>();
        for (Map.Entry<E, S> entry : sqlMap.entrySet()) {
            E conflictEnum = enumMap.put(entry.getValue(), entry.getKey());
            if (conflictEnum != null) {
                throw new IllegalStateException(
                        "Both '" +
                                entry.getKey() +
                                "' and '" +
                                conflictEnum +
                                "' are mapped as '" +
                                entry.getValue() +
                                "'"
                );
            }
        }
        return new EnumProvider<>(
                enumType,
                sqlType,
                enumMap,
                sqlMap
        );
    }

    private static class EnumProvider<E extends Enum<E>, S> extends ScalarProvider<E, S> {

        private final Map<S, E> enumMap;

        private final Map<E, S> sqlMap;

        public EnumProvider(
                Class<E> enumType,
                Class<S> sqlType,
                Map<S, E> enumMap,
                Map<E, S> sqlMap
        ) {
            super(enumType, sqlType);
            this.enumMap = enumMap;
            this.sqlMap = sqlMap;
        }

        @Override
        public @NotNull E toScalar(@NotNull S sqlValue) {
            E scalarValue = enumMap.get(sqlValue);
            if (scalarValue == null) {
                throw new IllegalArgumentException(
                        "Cannot resolve '" +
                                getScalarType() +
                                "' by the value '" +
                                sqlValue +
                                "'"
                );
            }
            return scalarValue;
        }

        @Override
        public @NotNull S toSql(@NotNull E enumValue) {
            S sqlValue = sqlMap.get(enumValue);
            if (sqlValue == null) {
                throw new AssertionError(
                        "Internal bug: Enum can be converted to sql value absolutely"
                );
            }
            return sqlValue;
        }
    }
}
