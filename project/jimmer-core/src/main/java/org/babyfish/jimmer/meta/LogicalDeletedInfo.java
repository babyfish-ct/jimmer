package org.babyfish.jimmer.meta;

import org.babyfish.jimmer.impl.util.Classes;
import org.babyfish.jimmer.impl.util.GenericValidator;
import org.babyfish.jimmer.lang.Ref;
import org.babyfish.jimmer.sql.Default;
import org.babyfish.jimmer.sql.JoinTable;
import org.babyfish.jimmer.sql.LogicalDeleted;
import org.babyfish.jimmer.sql.meta.LogicalDeletedLongGenerator;
import org.babyfish.jimmer.sql.meta.LogicalDeletedUUIDGenerator;
import org.babyfish.jimmer.sql.meta.LogicalDeletedValueGenerator;
import org.babyfish.jimmer.sql.meta.impl.MetadataLiterals;
import org.jetbrains.annotations.Nullable;

import java.time.*;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class LogicalDeletedInfo {

    private static final Map<Class<?>, Supplier<Object>> NOW_SUPPLIER_MAP;

    private final ImmutableProp prop;

    private final String columnName;

    private final Class<?> type;

    private final Action action;

    private final Object value;

    private final Class<? extends LogicalDeletedValueGenerator<?>> generatorType;

    private final String generatorRef;

    private final Ref<Object> initializedValueRef;

    private LogicalDeletedInfo(
            ImmutableProp prop,
            String columnName,
            Class<?> type,
            Action action,
            Object value,
            Class<? extends LogicalDeletedValueGenerator<?>> generatorType,
            String generatorRef,
            Ref<Object> initializedValueRef
    ) {
        this.prop = prop;
        this.columnName = columnName;
        this.type = type;
        this.action = action;
        this.value = value;
        this.generatorType = generatorType;
        this.generatorRef = generatorRef;
        this.initializedValueRef = initializedValueRef;
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
        this.columnName = base.columnName;
        this.type = base.type;
        this.action = base.action;
        this.value = base.value;
        this.generatorType = base.generatorType;
        this.generatorRef = base.generatorRef;
        this.initializedValueRef = base.initializedValueRef;
    }

    public ImmutableProp getProp() {
        return prop;
    }

    @Nullable
    public String getColumnName() {
        return columnName;
    }

    public Class<?> getType() {
        return type;
    }

    public Action getAction() {
        return action;
    }

    public boolean isDeleted(Object value) {
        LogicalDeletedInfo.Action reversedAction = this.action.reversed();
        if (reversedAction instanceof LogicalDeletedInfo.Action.Eq) {
            LogicalDeletedInfo.Action.Eq eq = (LogicalDeletedInfo.Action.Eq) reversedAction;
            return eq.getValue().equals(value);
        } else if (reversedAction instanceof LogicalDeletedInfo.Action.Ne) {
            LogicalDeletedInfo.Action.Ne ne = (LogicalDeletedInfo.Action.Ne) reversedAction;
            return !ne.getValue().equals(value);
        } else if (reversedAction instanceof LogicalDeletedInfo.Action.IsNull) {
            return value == null;
        } else if (reversedAction instanceof LogicalDeletedInfo.Action.IsNotNull) {
            return value != null;
        }
        throw new AssertionError("Internal bug: Unexpected logical deletion action: " + reversedAction);
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

    public boolean isInitializedValueSupported() {
        return initializedValueRef != null;
    }

    public Object allocateInitializedValue() {
        Ref<Object> ref = initializedValueRef;
        if (ref == null) {
            throw new IllegalArgumentException(
                    "The initialized value of the logical deleted column for \"" +
                            (columnName != null ? "The middle table of \"" + prop + "\"" : prop.toString()) +
                            "\" is not supported"
            );
        }
        Object value = ref.getValue();
        if (value instanceof Supplier<?>) {
            return ((Supplier<?>)value).get();
        }
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LogicalDeletedInfo that = (LogicalDeletedInfo) o;

        if (!prop.equals(that.prop)) return false;
        if (!Objects.equals(columnName, that.columnName)) return false;
        if (!action.equals(that.action)) return false;
        if (!Objects.equals(value, that.value)) return false;
        if (!Objects.equals(generatorType, that.generatorType))
            return false;
        return Objects.equals(generatorRef, that.generatorRef);
    }

    @Override
    public int hashCode() {
        int result = prop.hashCode();
        result = 31 * result + (columnName != null ? columnName.hashCode() : 0);
        result = 31 * result + action.hashCode();
        result = 31 * result + (value != null ? value.hashCode() : 0);
        result = 31 * result + (generatorType != null ? generatorType.hashCode() : 0);
        result = 31 * result + (generatorRef != null ? generatorRef.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "LogicalDeletedInfo{" +
                "prop=" + prop +
                ", columnName='" + columnName + '\'' +
                ", action=" + action +
                ", value=" + value +
                ", generatorType=" + generatorType +
                ", generatorRef='" + generatorRef + '\'' +
                '}';
    }

    public static LogicalDeletedInfo of(ImmutableProp prop) {
        LogicalDeleted deleted = prop.getAnnotation(LogicalDeleted.class);
        JoinTable.LogicalDeletedFilter deletedFilter = null;
        ImmutableProp filteredProp = prop.getMappedBy() != null ?
                prop.getMappedBy() :
                prop;
        JoinTable joinTable = filteredProp.getAnnotation(JoinTable.class);
        if (joinTable != null) {
            deletedFilter = joinTable.logicalDeletedFilter();
            if (deletedFilter.columnName().equals("<illegal-column-name>")) {
                deletedFilter = null;
            } else if (deletedFilter.columnName().isEmpty()) {
                throw new ModelException(
                        prefix(prop, deleted) +
                                "the \"columnName\" of " +
                                annotation(deleted) +
                                "cannot be empty"
                );
            }
        }
        if (deleted == null && deletedFilter == null) {
            return null;
        }
        Class<?> returnType = deletedFilter != null ? deletedFilter.type() : prop.getElementClass();
        if (deletedFilter != null) {
            if (returnType.isPrimitive() && deletedFilter.nullable()) {
                throw new ModelException(
                        prefix(prop, deleted) +
                                type(deleted) +
                                "is primitive type so that the `nullable` of " +
                                annotation(deleted) +
                                "must be false"
                );
            }
            if (Classes.primitiveTypeOf(returnType) != returnType && !deletedFilter.nullable()) {
                throw new ModelException(
                        prefix(prop, deleted) +
                                type(deleted) +
                                "is boxed type so that the `nullable` of " +
                                annotation(deleted) +
                                "must be true"
                );
            }
        }
        boolean isNullable = deleted != null ? prop.isNullable() : deletedFilter.nullable();
        if (returnType != boolean.class &&
                returnType != int.class &&
                returnType != long.class &&
                returnType != Long.class &&
                returnType != UUID.class &&
                !returnType.isEnum() &&
                !NOW_SUPPLIER_MAP.containsKey(returnType)) {
            throw new ModelException(
                    prefix(prop, deleted) +
                            type(deleted) +
                            "must be boolean, int, enum, long, Long, uuid or time"
            );
        }
        if (!isNullable && NOW_SUPPLIER_MAP.containsKey(returnType)) {
            throw new ModelException(
                    prefix(prop, deleted) +
                            type(deleted) +
                            "is " +
                            returnType.getName() +
                            "\" so that the argument `nullable` must be true"
            );
        }
        String initializedText = null;
        if (deletedFilter != null) {
            String v = deletedFilter.initializedValue();
            if (!v.isEmpty()) {
                initializedText = v;
            }
        } else {
            Default dft = prop.getAnnotation(Default.class);
            if (dft != null && !dft.value().isEmpty()) {
                initializedText = dft.value();
            }
        }

        String valueText = deleted != null ? deleted.value() : deletedFilter.value();
        if (valueText.isEmpty()) {
            valueText = null;
        } else if (initializedText != null && initializedText.equals(valueText)) {
            throw new ModelException(
                    prefix(prop, deleted) +
                            type(deleted) +
                            "is " +
                            returnType.getName() +
                            "\" so that it cannot be nullable"
            );
        }

        Class<? extends LogicalDeletedValueGenerator<?>> generatorType =
                deleted != null ? deleted.generatorType() : deletedFilter.generatorType();
        if (generatorType == LogicalDeletedValueGenerator.None.class) {
            generatorType = null;
        }
        String generatorRef = deleted != null ? deleted.generatorRef() : deletedFilter.generatorRef();
        if (generatorRef.isEmpty()) {
            generatorRef = null;
        }

        if (valueText != null && generatorType != null) {
            throw new ModelException(
                    prefix(prop, deleted) +
                            "`value` and `generatorType` of " +
                            annotation(deleted) +
                            "cannot be specified at the same time"
            );
        }
        if (valueText != null && generatorRef != null) {
            throw new ModelException(
                    prefix(prop, deleted) +
                            "`value` and `generatorRef` of " +
                            annotation(deleted) +
                            "cannot be specified at the same time"
            );
        }
        if (generatorType != null && generatorRef != null) {
            throw new ModelException(
                    prefix(prop, deleted) +
                            "`generatorType` and `generatorRef` of " +
                            annotation(deleted) +
                            "cannot be specified at the same time"
            );
        }

        Object value = valueText != null ? parseValue(prop, valueText, returnType) : null;
        Ref<Object> initializeValueRef =
                initializedText != null ?
                        Ref.of(
                                MetadataLiterals.valueOf(returnType, true, initializedText)
                        ) :
                        null;
        if (initializeValueRef == null) {
            if (value != null) {
                if (value instanceof Boolean) {
                    initializeValueRef = Ref.of(!(Boolean) value);
                } else if (value instanceof Integer && !value.equals(0)) {
                    initializeValueRef = Ref.of(0);
                } else if (value instanceof Long && !value.equals(0L)) {
                    initializeValueRef = Ref.of(0L);
                } else if (value instanceof Enum<?> && value.getClass().getEnumConstants().length == 2) {
                    String name = ((Enum<?>) value).name();
                    for (Object constant : value.getClass().getEnumConstants()) {
                        if (!((Enum<?>) constant).name().equals(name)) {
                            initializeValueRef = Ref.of(constant);
                            break;
                        }
                    }
                } else if (valueText.equals("null") && NOW_SUPPLIER_MAP.containsKey(returnType)) {
                    initializeValueRef = Ref.of(MetadataLiterals.valueOf(returnType, isNullable, "now"));
                } else if (valueText.equals("now") && isNullable) {
                    initializeValueRef = Ref.of(null);
                }
            } else {
                if (isNullable) {
                    initializeValueRef = Ref.of(null);
                } else if (returnType == boolean.class) {
                    initializeValueRef = Ref.of(false);
                } else if (returnType == int.class) {
                    initializeValueRef = Ref.of(0);
                } else if (returnType == long.class) {
                    initializeValueRef = Ref.of(0L);
                } else if (returnType == UUID.class) {
                    initializeValueRef = Ref.of(UUID.fromString("00000000-0000-0000-0000-000000000000"));
                }
            }
        }
        if (initializeValueRef == null) {
            throw new ModelException(
                    prefix(prop, deleted) +
                            "The initialized value of " +
                            annotation(deleted) +
                            "must be specified because it cannot be determined automatically"
            );
        }

        if (returnType == long.class || returnType == Long.class || returnType == UUID.class) {
            if (valueText != null) {
                throw new ModelException(
                        prefix(prop, deleted) +
                                ", " +
                                type(deleted) +
                                " is " +
                                returnType +
                                " so that the `value` of " +
                                annotation(deleted) +
                                "cannot be specified"
                );
            }
            if (generatorType != null) {
                new GenericValidator(
                        prop,
                        deletedFilter != null ?
                                JoinTable.LogicalDeletedFilter.class :
                                LogicalDeleted.class,
                        generatorType,
                        LogicalDeletedValueGenerator.class
                )
                        .expect(0, returnType)
                        .validate();
            }
            if (generatorType == null && generatorRef == null) {
                if (returnType == long.class) {
                    generatorType = LogicalDeletedLongGenerator.class;
                } else if (returnType == UUID.class) {
                    generatorType = LogicalDeletedUUIDGenerator.class;
                } else {
                    throw new ModelException(
                            prefix(prop, deleted) +
                                    ", " +
                                    type(deleted) +
                                    " is " +
                                    returnType +
                                    " so that the `generatorType` or `generatorRef` of " +
                                    annotation(deleted) +
                                    "must be specified"
                    );
                }
            }
            if (isNullable) {
                return new LogicalDeletedInfo(
                        deletedFilter != null ? filteredProp : prop,
                        deletedFilter != null ? deletedFilter.columnName() : null,
                        deletedFilter != null ? deletedFilter.type() : prop.getReturnClass(),
                        Action.IsNull.INSTANCE,
                        null,
                        generatorType,
                        generatorRef,
                        initializeValueRef
                );
            }
            return new LogicalDeletedInfo(
                    deletedFilter != null ? filteredProp : prop,
                    deletedFilter != null ? deletedFilter.columnName() : null,
                    deletedFilter != null ? deletedFilter.type() : prop.getReturnClass(),
                    new Action.Eq(initializeValueRef.getValue()),
                    null,
                    generatorType,
                    generatorRef,
                    initializeValueRef
            );
        }

        if (valueText == null) {
            throw new ModelException(
                    prefix(prop, deleted) +
                            ", " +
                            type(deleted) +
                            " is " +
                            returnType +
                            " so that the `value` of " +
                            annotation(deleted) +
                            "must be specified"
            );
        }
        Action action;
        if (isNullable) {
            action = value != null ? Action.IsNull.INSTANCE : Action.IsNotNull.INSTANCE;
        } else {
            action = new Action.Ne(value);
        }
        return new LogicalDeletedInfo(
                deletedFilter != null ? filteredProp : prop,
                deletedFilter != null ? deletedFilter.columnName() : null,
                deletedFilter != null ? deletedFilter.type() : prop.getReturnClass(),
                action,
                value,
                null,
                null,
                initializeValueRef
        );
    }

    @SuppressWarnings("unchecked")
    private static Object parseValue(ImmutableProp prop, String value, Class<?> type) {
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

    private static String prefix(ImmutableProp prop, LogicalDeleted deleted) {
        if (deleted != null) {
            return "Illegal property \"" +
                    prop +
                    "\", it is decorated by \"@" +
                    LogicalDeleted.class.getName() +
                    "\", ";
        }
        return "Illegal property \"" +
                (prop.getMappedBy() != null ? prop.getMappedBy() : prop) +
                "\", it is decorated by \"@" +
                JoinTable.class.getName() +
                "\" whose argument `logicalDeletedFilter` is specified as an annotation \"@" +
                JoinTable.LogicalDeletedFilter.class.getName() +
                "\", ";
    }

    private static String type(LogicalDeleted deleted) {
        if (deleted != null) {
            return "the return type of the property ";
        }
        return "the type of the filtered column ";
    }

    private static String annotation(LogicalDeleted deleted) {
        return "\"@" +
                (deleted != null ? LogicalDeleted.class : JoinTable.LogicalDeletedFilter.class) +
                "\" ";
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
        map.put(Instant.class, Instant::now);
        NOW_SUPPLIER_MAP = map;
    }
}
