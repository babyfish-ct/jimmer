package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.meta.SimpleType;
import org.babyfish.jimmer.client.meta.Visitor;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

class SimpleTypeImpl implements SimpleType {

    private static final Map<Class<?>, SimpleType> SIMPLE_TYPE_MAP;

    private final Class<?> javaType;

    @Nullable
    public static SimpleType get(Class<?> rawType) {
        return SIMPLE_TYPE_MAP.get(rawType);
    }

    private SimpleTypeImpl(Class<?> javaType) {
        this.javaType = javaType;
    }

    @Override
    public Class<?> getJavaType() {
        return javaType;
    }

    @Override
    public void accept(Visitor visitor) {
        if (visitor.isTypeVisitable(this)) {
            visitor.visitSimpleType(this);
        }
    }

    @Override
    public String toString() {
        return javaType.getSimpleName();
    }

    static {
        Class<?>[] arr = new Class<?>[] {
                void.class,
                Object.class,
                boolean.class, Boolean.class,
                char.class, Character.class,
                byte.class, Byte.class,
                short.class, Short.class,
                int.class, Integer.class,
                long.class, Long.class,
                float.class, Float.class,
                double.class, Double.class,
                BigInteger.class,
                BigDecimal.class,
                String.class,
                UUID.class,
                java.util.Date.class,
                java.sql.Date.class,
                java.sql.Time.class,
                java.sql.Timestamp.class,
                LocalDate.class,
                LocalTime.class,
                LocalDateTime.class,
                OffsetDateTime.class,
                ZonedDateTime.class
        };
        Map<Class<?>, SimpleType> map = new HashMap<>((arr.length * 4 + 2) / 3);
        for (Class<?> clazz : arr) {
            map.put(clazz, new SimpleTypeImpl(clazz));
        }
        SIMPLE_TYPE_MAP = map;
    }
}
