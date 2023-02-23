package org.babyfish.jimmer.meta;

import org.babyfish.jimmer.sql.LogicalDeleted;

import java.time.*;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class LogicalDeletedInfo {

    private static final Map<Class<?>, Supplier<Object>> NOW_SUPPLIER_MAP;

    private final ImmutableProp prop;

    private final Object value;

    private final Object restoredValue;

    private final boolean isTwoOptionsOnly;

    private LogicalDeletedInfo(
            ImmutableProp prop,
            Object value,
            Object restoredValue
    ) {
        if (value == restoredValue) {
            throw new IllegalArgumentException("`value` and `restoredValue` must be different");
        }
        this.prop = prop;
        this.value = value;
        this.restoredValue = restoredValue;
        Class<?> returnType = prop.getElementClass();
        this.isTwoOptionsOnly =
                returnType == boolean.class ||
                        (returnType.isEnum() && returnType.getEnumConstants().length == 2);
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
        this.value = base.value;
        this.restoredValue = base.restoredValue;
        this.isTwoOptionsOnly = base.isTwoOptionsOnly;
    }

    public ImmutableProp getProp() {
        return prop;
    }

    public Action getNotDeletedAction() {
        if (value != null && restoredValue != null) {
            return Action.NE;
        }
        if (value == null) {
            return Action.IS_NOT_NULL;
        }
        return Action.IS_NULL;
    }

    public boolean isTwoOptionsOnly() {
        return isTwoOptionsOnly;
    }

    public boolean isMultiViewCacheUsed() {
        return prop.getAnnotation(LogicalDeleted.class).useMultiViewCache();
    }

    public Object getValue() {
        if (value instanceof Supplier<?>) {
            return ((Supplier<?>)value).get();
        }
        return value;
    }

    public Object getRestoredValue() {
        if (restoredValue instanceof Supplier<?>) {
            return ((Supplier<?>)restoredValue).get();
        }
        return restoredValue;
    }

    @Override
    public String toString() {
        return "LogicalDeletedFlag{" +
                "prop=" + prop +
                ", value=" + (value instanceof Supplier ? "<lambda>" : value) +
                ", restoredValue=" + (value instanceof Supplier ? "<lambda>" : value) +
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
                        !returnType.isEnum() &&
                        !NOW_SUPPLIER_MAP.containsKey(returnType))) {
            throw new ModelException(
                    "Illegal property \"" +
                            prop +
                            "\", it is decorated by `@" +
                            LogicalDeleted.class.getName() +
                            "` so that it type must be boolean, integer, enum or time"
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
        } else if (prop.isNullable()) {
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

        if (deleted.value().isEmpty()) {
            throw new ModelException(
                    "Illegal property \"" +
                            prop +
                            "\", it is decorated by `@" +
                            LogicalDeleted.class.getName() +
                            "` but the `value` is empty string"
            );
        }

        if (deleted.value().equals(deleted.restoredValue())) {
            throw new ModelException(
                    "Illegal property \"" +
                            prop +
                            "\", it is decorated by `@" +
                            LogicalDeleted.class.getName() +
                            "` but the `value` and `restoredValue` are same"
            );
        }

        Object value = parseValue(prop, deleted.value(), "value");
        Object restoredValue = deleted.restoredValue().isEmpty() ?
                null :
                parseValue(prop, deleted.restoredValue(), "restoredValue");
        if (restoredValue == null) {
            if (returnType == boolean.class) {
                restoredValue = !(Boolean) value;
            } else if (NOW_SUPPLIER_MAP.containsKey(returnType)) {
                if (value == null) {
                    restoredValue = NOW_SUPPLIER_MAP.get(returnType);
                }
            } else {
                if (returnType.isEnum()) {
                    Enum<?>[] constants = ((Class<Enum<?>>) returnType).getEnumConstants();
                    if (constants.length == 2) {
                        if (value == constants[0]) {
                            restoredValue = constants[1];
                        } else {
                            restoredValue = constants[0];
                        }
                    }
                }
                throw new ModelException(
                        "Illegal property \"" +
                                prop +
                                "\", it is decorated by `@" +
                                LogicalDeleted.class.getName() +
                                "` and returns \"" +
                                returnType.getName() +
                                "\" so that the `restoredValue` is required"
                );
            }
        }
        return new LogicalDeletedInfo(prop, value, restoredValue);
    }

    @SuppressWarnings("unchecked")
    private static Object parseValue(ImmutableProp prop, String value, String argumentName) {
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
                                    "` and its type is boolean, but the `" +
                                    argumentName +
                                    "` is \"" +
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
                                "` and its type is int, but the `" +
                                argumentName +
                                "` is \"" +
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
                            "\", but the `" +
                            argumentName +
                            "` is \"" +
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
                                    "\", but the `" +
                                    argumentName +
                                    "` is \"" +
                                    value +
                                    "\" which is neither \"null\" or \"now\""
                    );
            }
        }
    }

    public enum Action {
        NE,
        IS_NULL,
        IS_NOT_NULL
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
