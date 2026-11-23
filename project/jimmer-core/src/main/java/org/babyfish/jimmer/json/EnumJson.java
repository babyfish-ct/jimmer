package org.babyfish.jimmer.json;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class EnumJson {

    private static final ClassValue<Optional<EnumJson>> CACHE = new ClassValue<Optional<EnumJson>>() {
        @Override
        protected Optional<EnumJson> computeValue(Class<?> type) {
            Method accessor = null;
            for (Method method : type.getDeclaredMethods()) {
                if (method.isBridge() || method.isSynthetic() || !method.isAnnotationPresent(JsonValue.class)) {
                    continue;
                }
                if (accessor != null) {
                    throw new IllegalArgumentException("Multiple @JsonValue methods on " + type.getName());
                }
                Class<?> result = method.getReturnType();
                if (!Modifier.isPublic(method.getModifiers()) || Modifier.isStatic(method.getModifiers()) ||
                        method.getParameterCount() != 0 || (result != String.class && !isIntegral(result))) {
                    throw new IllegalArgumentException("Invalid @JsonValue method " + method);
                }
                accessor = method;
            }
            return accessor == null ? Optional.empty() : Optional.of(new EnumJson(type, accessor));
        }
    };

    /** Returns the scalar mapping of an enum, or null if the type has none. */
    public static EnumJson find(Class<?> type) {
        if (type.isEnum()) {
            return CACHE.get(type).orElse(null);
        }
        Class<?> parent = type.getSuperclass();
        return parent != null && parent.isEnum() ? CACHE.get(parent).orElse(null) : null;
    }

    private final Class<?> type;

    private final boolean numeric;

    private final Map<Object, Enum<?>> constants = new HashMap<>();

    private final Map<Enum<?>, Object> values = new HashMap<>();

    private EnumJson(Class<?> type, Method accessor) {
        this.type = type;
        this.numeric = accessor.getReturnType() != String.class;
        if (!Modifier.isPublic(type.getModifiers())) {
            accessor.setAccessible(true);
        }
        for (Object constant : type.getEnumConstants()) {
            Object value;
            try {
                value = accessor.invoke(constant);
            } catch (ReflectiveOperationException ex) {
                throw new IllegalArgumentException("Cannot read @JsonValue from " + type.getName(), ex);
            }
            if (value == null) {
                throw new IllegalArgumentException("Null @JsonValue on " + type.getName() + "." + ((Enum<?>) constant).name());
            }
            Object key = numeric ? ((Number) value).longValue() : value;
            if (constants.put(key, (Enum<?>) constant) != null) {
                throw new IllegalArgumentException("Duplicate @JsonValue " + value + " on " + type.getName());
            }
            values.put((Enum<?>) constant, value);
        }
    }

    public Object encode(Enum<?> constant) {
        if (constant.getDeclaringClass() != type) {
            throw new IllegalArgumentException("Expected an enum constant of " + type.getName());
        }
        return values.get(constant);
    }

    public Enum<?> decode(Object value) {
        Object key = value;
        if (numeric && value instanceof Number) {
            try {
                key = new BigDecimal(value.toString()).longValueExact();
            } catch (NumberFormatException | ArithmeticException ex) {
                throw new IllegalArgumentException("Invalid @JsonValue " + value + " for " + type.getName(), ex);
            }
        }
        Enum<?> constant = constants.get(key);
        if (constant == null) {
            throw new IllegalArgumentException("Unknown @JsonValue " + value + " for " + type.getName());
        }
        return constant;
    }

    private static boolean isIntegral(Class<?> type) {
        return type == byte.class || type == Byte.class || type == short.class || type == Short.class ||
                type == int.class || type == Integer.class || type == long.class || type == Long.class;
    }
}
