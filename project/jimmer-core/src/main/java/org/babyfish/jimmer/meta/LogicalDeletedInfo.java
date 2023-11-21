package org.babyfish.jimmer.meta;

import org.babyfish.jimmer.impl.util.GenericValidator;
import org.babyfish.jimmer.sql.LogicalDeleted;
import org.babyfish.jimmer.sql.meta.LogicalDeletedUUIDGenerator;
import org.babyfish.jimmer.sql.meta.LogicalDeletedValueGenerator;

import java.time.*;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class LogicalDeletedInfo {

    private static final Map<Class<?>, Supplier<Object>> NOW_SUPPLIER_MAP;

    private final ImmutableProp prop;

    private final Action action;

    private final Object value;

    private final Class<? extends LogicalDeletedValueGenerator<?>> generatorType;

    private final String generatorRef;

    private LogicalDeletedInfo(
            ImmutableProp prop,
            Action action,
            Object value,
            Class<? extends LogicalDeletedValueGenerator<?>> generatorType,
            String generatorRef
    ) {
        this.prop = prop;
        this.action = action;
        this.value = value;
        this.generatorType = generatorType;
        this.generatorRef = generatorRef;
    }

    public LogicalDeletedInfo to(ImmutableProp prop) {
        if (this.prop == prop) {
            return this;
        }
        return new LogicalDeletedInfo(this, prop);
    }

    private LogicalDeletedInfo(LogicalDeletedInfo base, ImmutableProp prop) {
        if (!base.prop.getName().equals(prop.getName()) ||
                !base.prop.getDeclaringType().isAssignableFrom(prop.getDeclaringType())) {
            throw new IllegalArgumentException(
                    "\"" +
                            prop +
                            "\" does not hide \"" +
                            base.prop +
                            "\""
            );
        }
        this.prop = prop;
        this.action = base.action;
        this.value = base.value;
        this.generatorType = base.generatorType;
        this.generatorRef = base.generatorRef;
    }

    public ImmutableProp getProp() {
        return prop;
    }

    public Action getAction() {
        return action;
    }

    public Object generateValue() {
        if (generatorType != null || generatorRef != null) {
            throw new AssertionError("Internal bug, cannot generate value, please use generator");
        }
        if (value instanceof Supplier<?>) {
            return ((Supplier<?>)value).get();
        }
        return value;
    }

    public Class<? extends LogicalDeletedValueGenerator<?>> getGeneratorType() {
        return generatorType;
    }

    public String getGeneratorRef() {
        return generatorRef;
    }

    @Override
    public String toString() {
        return "LogicalDeletedInfo{" +
                "prop=" + prop +
                ", action=" + action +
                ", value=" + value +
                '}';
    }

    public static LogicalDeletedInfo of(ImmutableProp prop) {
        LogicalDeleted deleted = prop.getAnnotation(LogicalDeleted.class);
        if (deleted == null) {
            return null;
        }
        Class<?> returnType = prop.getElementClass();
        if (prop.isAssociation(TargetLevel.OBJECT) || (
                returnType != boolean.class &&
                        returnType != int.class &&
                        returnType != long.class &&
                        returnType != Long.class &&
                        returnType != UUID.class &&
                        !returnType.isEnum() &&
                        !NOW_SUPPLIER_MAP.containsKey(returnType))) {
            throw new ModelException(
                    "Illegal property \"" +
                            prop +
                            "\", it is decorated by `@" +
                            LogicalDeleted.class.getName() +
                            "` so that it type must be boolean, integer, enum, long, long, uuid or time"
            );
        }
        if (NOW_SUPPLIER_MAP.containsKey(returnType)) {
            if (!prop.isNullable()) {
                throw new ModelException(
                        "Illegal property \"" +
                                prop +
                                "\", it is decorated by `@" +
                                LogicalDeleted.class.getName() +
                                "` and returns \"" +
                                returnType.getName() +
                                "\" so that it must be nullable"
                );
            }
        } else if (returnType != long.class && returnType != Long.class && returnType != UUID.class && prop.isNullable()) {
            throw new ModelException(
                    "Illegal property \"" +
                            prop +
                            "\", it is decorated by `@" +
                            LogicalDeleted.class.getName() +
                            "` and returns \"" +
                            returnType.getName() +
                            "\" so that it cannot be nullable"
            );
        }

        String valueText = deleted.value();
        if (valueText.isEmpty()) {
            valueText = null;
        }
        Class<? extends LogicalDeletedValueGenerator<?>> generatorType = deleted.generatorType();
        if (generatorType == LogicalDeletedValueGenerator.None.class) {
            generatorType = null;
        }
        String generatorRef = deleted.generatorRef();
        if (generatorRef.isEmpty()) {
            generatorRef = null;
        }

        if (valueText != null && generatorType != null) {
            throw new ModelException(
                    "Illegal property \"" +
                            prop +
                            "\", `value` and `generatorType` of `@LogicalDeleted` cannot be specified at the same time"
            );
        }
        if (valueText != null && generatorRef != null) {
            throw new ModelException(
                    "Illegal property \"" +
                            prop +
                            "\", `value` and `generatorRef` of `@LogicalDeleted` cannot be specified at the same time"
            );
        }
        if (generatorType != null && generatorRef != null) {
            throw new ModelException(
                    "Illegal property \"" +
                            prop +
                            "\", `generatorType` and `generatorRef` of `@LogicalDeleted` cannot be specified at the same time"
            );
        }

        if (returnType == long.class || returnType == Long.class || returnType == UUID.class) {
            if (valueText != null) {
                throw new ModelException(
                        "Illegal property \"" +
                                prop +
                                "\", the property returns \"" +
                                returnType +
                                "\" does not require `value` of `@LogicalDeleted`"
                );
            }
            if (generatorType != null) {
                new GenericValidator(prop, LogicalDeleted.class, generatorType, LogicalDeletedValueGenerator.class)
                        .expect(0, UUID.class)
                        .validate();
            }
            if (generatorType == null && generatorRef == null) {
                if (returnType == UUID.class) {
                    generatorType = LogicalDeletedUUIDGenerator.class;
                } else {
                    throw new ModelException(
                            "Illegal property \"" +
                                    prop +
                                    "\", the property returns \"" +
                                    returnType +
                                    "\" requires `generatorType` or `generatorRef` of `@LogicalDeleted`"
                    );
                }
            }
            if (prop.isNullable()) {
                return new LogicalDeletedInfo(prop, Action.IsNull.INSTANCE, null, generatorType, generatorRef);
            }
            Object notDeletedValue = returnType == UUID.class ?
                    UUID.fromString("00000000-0000-0000-0000-000000000000") :
                    0L;
            return new LogicalDeletedInfo(
                    prop,
                    new Action.Eq(notDeletedValue),
                    null,
                    generatorType,
                    generatorRef
            );
        }

        if (deleted.value().isEmpty()) {
            throw new ModelException(
                    "Illegal property \"" +
                            prop +
                            "\", the property returns \"" +
                            returnType +
                            "\" requires `value` of `@LogicalDeleted`"
            );
        }
        Object value = parseValue(prop, deleted.value());
        Action action;
        if (prop.isNullable()) {
            action = value != null ? Action.IsNull.INSTANCE : Action.IsNotNull.INSTANCE;
        } else {
            action = new Action.Ne(value);
        }
        return new LogicalDeletedInfo(prop, action, value, null, null);
    }

    @SuppressWarnings("unchecked")
    private static Object parseValue(ImmutableProp prop, String value) {
        Class<?> type = prop.getElementClass();
        if (type == boolean.class) {
            switch (value) {
                case "true":
                    return true;
                case "false":
                    return false;
                default:
                    throw new ModelException(
                            "Illegal property \"" +
                                    prop +
                                    "\", it is decorated by `@" +
                                    LogicalDeleted.class.getName() +
                                    "` and its type is boolean, but the `value` is \"" +
                                    value +
                                    "\" is neither \"true\" nor \"false\""
                    );
            }
        } else if (type == int.class) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ex) {
                throw new ModelException(
                        "Illegal property \"" +
                                prop +
                                "\", it is decorated by `@" +
                                LogicalDeleted.class.getName() +
                                "` and its type is int, but the `value` is \"" +
                                value +
                                "\" which is not a valid integer"
                );
            }
        } else if (type.isEnum()) {
            Enum<?>[] constants = ((Class<Enum<?>>)type).getEnumConstants();
            for (Enum<?> constant : constants) {
                if (constant.name().equals(value)) {
                    return constant;
                }
            }
            throw new ModelException(
                    "Illegal property \"" +
                            prop +
                            "\", it is decorated by `@" +
                            LogicalDeleted.class.getName() +
                            "` and its type is the enum type \"" +
                            type.getName() +
                            "\", but the `value` is \"" +
                            value +
                            "\" which is not any one of: " +
                            Arrays.stream(constants).map(Enum::name).collect(Collectors.toList())
            );
        } else {
            switch (value) {
                case "null":
                    return null;
                case "now":
                    return NOW_SUPPLIER_MAP.get(type);
                default:
                    throw new ModelException(
                            "Illegal property \"" +
                                    prop +
                                    "\", it is decorated by `@" +
                                    LogicalDeleted.class.getName() +
                                    "` and its type is the time type \"" +
                                    type.getName() +
                                    "\", but the `value` is \"" +
                                    value +
                                    "\" which is neither \"null\" or \"now\""
                    );
            }
        }
    }

    public static abstract class Action {

        Action() {}

        public abstract Action reversed();

        public static class Eq extends Action {

            private final Object value;

            private Action reversed;

            Eq(Object value) {
                this.value = value;
            }

            public Object getValue() {
                return value;
            }

            @Override
            public Action reversed() {
                if (reversed == null) {
                    reversed = new Ne(value);
                }
                return reversed;
            }
        }

        public static class Ne extends Action {

            private final Object value;

            private Action reversed;

            Ne(Object value) {
                this.value = value;
            }

            public Object getValue() {
                return value;
            }

            @Override
            public Action reversed() {
                if (reversed == null) {
                    reversed = new Eq(value);
                }
                return reversed;
            }
        }

        public static class IsNull extends Action {

            static final IsNull INSTANCE = new IsNull();

            private IsNull() {}

            @Override
            public Action reversed() {
                return IsNotNull.INSTANCE;
            }
        }

        public static class IsNotNull extends Action {

            static final IsNotNull INSTANCE = new IsNotNull();

            private IsNotNull() {}

            @Override
            public Action reversed() {
                return IsNull.INSTANCE;
            }
        }
    }

    static {
        Map<Class<?>, Supplier<Object>> map = new HashMap<>();
        map.put(java.util.Date.class, java.util.Date::new);
        map.put(java.sql.Date.class, () -> new java.sql.Date(System.currentTimeMillis()));
        map.put(java.sql.Time.class, () -> new java.sql.Timestamp(System.currentTimeMillis()));
        map.put(java.sql.Timestamp.class, () -> new java.sql.Timestamp(System.currentTimeMillis()));
        map.put(LocalDateTime.class, LocalDateTime::now);
        map.put(LocalDate.class, LocalDate::now);
        map.put(LocalTime.class, LocalTime::now);
        map.put(OffsetDateTime.class, OffsetDateTime::now);
        map.put(ZonedDateTime.class, ZonedDateTime::now);
        NOW_SUPPLIER_MAP = map;
    }
}
