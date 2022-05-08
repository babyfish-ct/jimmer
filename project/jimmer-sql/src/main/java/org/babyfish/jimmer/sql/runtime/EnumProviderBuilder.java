package org.babyfish.jimmer.sql.runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class EnumProviderBuilder<E extends Enum<E>, S> {

    private Class<E> enumType;

    private Class<S> sqlType;

    private Function<E, S> defaultSqlValueMapper;

    private Map<E, S> sqlMap = new HashMap<>();

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

    public EnumProviderBuilder<E, S> map(E enumValue, S sqlValue) {
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

        private Map<S, E> enumMap;

        private Map<E, S> sqlMap;

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
        public E toScalar(S sqlValue) {
            E scalarValue = enumMap.get(sqlValue);
            if (scalarValue == null) {
                throw new IllegalArgumentException(
                        "Cannot resolve '$" +
                                getScalarType().getName() +
                                "' by the value '" +
                                sqlValue +
                                "'"
                );
            }
            return scalarValue;
        }

        @Override
        public S toSql(E enumValue) {
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

/*
class EnumProviderBuilder<E: Enum<E>, S: Any> internal constructor(
    private val scalarType: KClass<E>,
    private val sqlType: KClass<S>,
    private val defaultSqlValue: (E) -> S
) {

    private val sqlMap = mutableMapOf<E, S>()

    fun map(enumValue: E, sqlValue: S) {
        if (sqlMap.contains(enumValue)) {
            error("'${enumValue}' has been mapped")
        }
        sqlMap[enumValue] = sqlValue
    }

    internal fun build(): ScalarProvider<E, S> {
        val sqlMap = sqlMap.toMutableMap()
        for (enumValue in scalarType.java.enumConstants) {
            sqlMap.computeIfAbsent(enumValue, defaultSqlValue)
        }
        val enumMap = mutableMapOf<S, E>()
        for (e in sqlMap.entries) {
            val conflictEnum = enumMap.put(e.value, e.key)
            if (conflictEnum !== null) {
                error("Both '${e.key}' and '${conflictEnum}' are mapped as '${e.value}'")
            }
        }
        return EnumProvider(
            scalarType,
            sqlType,
            enumMap,
            sqlMap
        )
    }
}
 */