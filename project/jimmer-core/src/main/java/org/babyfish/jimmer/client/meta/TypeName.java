package org.babyfish.jimmer.client.meta;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

@com.fasterxml.jackson.databind.annotation.JsonSerialize(using = TypeName.SerializerV2.class)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = TypeName.DeserializerV2.class)
@tools.jackson.databind.annotation.JsonSerialize(using = TypeName.SerializerV3.class)
@tools.jackson.databind.annotation.JsonDeserialize(using = TypeName.DeserializerV3.class)
public class TypeName implements Comparable<TypeName> {

    public static final TypeName VOID = new TypeName(null, "void");

    public static final TypeName BOOLEAN = new TypeName(null, "boolean") {
        @Override
        public TypeName box() {
            return new TypeName("java.lang", "Boolean");
        }
    };

    public static final TypeName CHAR = new TypeName(null, "char") {
        @Override
        public TypeName box() {
            return new TypeName("java.lang", "Character");
        }
    };

    public static final TypeName BYTE = new TypeName(null, "byte") {
        @Override
        public TypeName box() {
            return new TypeName("java.lang", "Byte");
        }
    };

    public static final TypeName SHORT = new TypeName(null, "short") {
        @Override
        public TypeName box() {
            return new TypeName("java.lang", "Short");
        }
    };

    public static final TypeName INT = new TypeName(null, "int") {
        @Override
        public TypeName box() {
            return new TypeName("java.lang", "Integer");
        }
    };

    public static final TypeName LONG = new TypeName(null, "long") {
        @Override
        public TypeName box() {
            return new TypeName("java.lang", "Long");
        }
    };

    public static final TypeName FLOAT = new TypeName(null, "float") {
        @Override
        public TypeName box() {
            return new TypeName("java.lang", "Float");
        }
    };

    public static final TypeName DOUBLE = new TypeName(null, "double") {
        @Override
        public TypeName box() {
            return new TypeName("java.lang", "Double");
        }
    };

    public static final TypeName OBJECT = new TypeName("java.lang", "Object");

    public static final TypeName STRING = new TypeName("java.lang", "String");

    public static final TypeName LIST = new TypeName("java.util", "List");

    public static final TypeName MAP = new TypeName("java.util", "Map");

    public static final TypeName OPTIONAL = new TypeName("java.util", "Optional");

    private static final Pattern DOLLAR_PATTERN = Pattern.compile("\\$");

    @Nullable
    private final String packageName;

    private final List<String> simpleNames;

    @Nullable
    private final String typeVariable;

    private String javaString;

    private String jvmString;

    private TypeName(@Nullable String packageName, String simpleName) {
        this(packageName, Collections.singletonList(simpleName), null);
    }

    private TypeName(@Nullable String packageName, List<String> simpleNames) {
        this(packageName, simpleNames, null);
    }

    private TypeName(@Nullable String packageName, List<String> simpleNames, @Nullable String typeVariable) {
        this.packageName = packageName != null && !packageName.isEmpty() ? packageName : null;
        this.simpleNames = Collections.unmodifiableList(simpleNames);
        this.typeVariable = typeVariable;
    }

    public static TypeName of(String packageName, List<String> simpleNames) {
        if (packageName != null && !packageName.isEmpty() && simpleNames.size() == 1) {
            switch (packageName + '.' + simpleNames.get(0)) {
                case "void":
                case "kotlin.Unit":
                case "kotlin.Nothing":
                    return VOID;
                case "boolean":
                case "java.lang.Boolean":
                case "kotlin.Boolean":
                    return BOOLEAN;
                case "char":
                case "java.lang.Character":
                case "kotlin.Char":
                    return CHAR;
                case "byte":
                case "java.lang.Byte":
                case "kotlin.Byte":
                    return BYTE;
                case "short":
                case "java.lang.Short":
                case "kotlin.Short":
                    return SHORT;
                case "int":
                case "java.lang.Integer":
                case "kotlin.Int":
                    return INT;
                case "long":
                case "java.lang.Long":
                case "kotlin.Long":
                    return LONG;
                case "float":
                case "java.lang.Float":
                case "kotlin.Float":
                    return FLOAT;
                case "double":
                case "java.lang.Double":
                case "kotlin.Double":
                    return DOUBLE;
                case "java.lang.Object":
                case "kotlin.Any":
                    return OBJECT;
                case "java.lang.String":
                case "kotlin.String":
                    return STRING;
                case "java.util.Iterable":
                case "java.util.Collection":
                case "java.util.List":
                case "java.util.Set":
                case "java.util.SortedSet":
                case "java.util.NavigableSet":
                case "java.util.SequencedSet":
                case "kotlin.collections.Iterable":
                case "kotlin.collections.Collection":
                case "kotlin.collections.List":
                case "kotlin.collections.Set":
                case "kotlin.collections.MutableIterable":
                case "kotlin.collections.MutableCollection":
                case "kotlin.collections.MutableList":
                case "kotlin.collections.MutableSet":
                case "kotlin.Array":
                    return LIST;
                case "java.util.Map":
                case "java.util.SortedMap":
                case "java.util.NavigableMap":
                case "java.util.SequencedMap":
                case "kotlin.collections.Map":
                case "kotlin.collections.MutableMap":
                    return MAP;
            }
        }
        return new TypeName(packageName, simpleNames);
    }

    public static TypeName of(Class<?> type) {
        Package pkg = type.getPackage();
        List<String> simpleNames = new ArrayList<>();
        while (type != null) {
            simpleNames.add(0, type.getSimpleName());
            type = type.getDeclaringClass();
        }
        return of(pkg == null ? null : pkg.getName(), simpleNames);
    }

    public TypeName typeVariable(String typeVariable) {
        return new TypeName(packageName, new ArrayList<>(simpleNames), typeVariable);
    }

    @Nullable
    public String getPackageName() {
        return packageName;
    }

    @NotNull
    public List<String> getSimpleNames() {
        return simpleNames;
    }

    @Nullable
    public String getTypeVariable() {
        return typeVariable;
    }

    public boolean isPrimitive() {
        if (packageName == null && simpleNames.size() == 1) {
            switch (simpleNames.get(0)) {
                case "boolean":
                case "char":
                case "byte":
                case "short":
                case "int":
                case "long":
                case "float":
                case "double":
                    return true;
            }
        }
        return false;
    }

    public boolean isGenerationRequired() {
        String text = toString();
        switch (text) {
            case "boolean":
            case "char":
            case "byte":
            case "short":
            case "int":
            case "long":
            case "float":
            case "double":
            case "java.lang.Object":
            case "java.io.Closeable":
            case "java.lang.AutoCloseable":
            case "java.lang.Enum":
            case "java.lang.Class":
            case "java.math.BigDecimal":
            case "java.math.BigInteger":
            case "java.util.Iterable":
            case "java.util.Collection":
            case "java.util.List":
            case "java.util.Set":
            case "java.util.SortedSet":
            case "java.util.NavigableSet":
            case "java.util.SequencedSet":
            case "java.util.Map":
            case "java.util.SortedMap":
            case "java.util.NavigableMap":
            case "java.util.SequencedMap":
            case "java.lang.String":
            case "java.util.UUID":
            case "java.util.Date":
            case "java.sql.Date":
            case "java.sql.Time":
            case "java.sql.Timestamp":
            case "java.time.Instant":
            case "java.time.LocalTime":
            case "java.time.LocalDate":
            case "java.time.LocalDateTime":
            case "java.time.OffsetDateTime":
            case "java.time.ZonedDateTime":
                return false;
            default:
                return !text.startsWith("<");
        }
    }

    public TypeName box() {
        return this;
    }

    @Override
    public int compareTo(@NotNull TypeName o) {
        return toString().compareTo(o.toString());
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TypeName typeName = (TypeName) o;
        return toString().equals(typeName.toString());
    }

    @Override
    public String toString() {
        return toString(false);
    }

    public String toString(boolean jvmStyle) {
        String str;
        if (jvmStyle) {
            str = jvmString;
            if (str == null) {
                jvmString = str = toStringImpl(true);
            }
        } else {
            str = javaString;
            if (str == null) {
                javaString = str = toStringImpl(false);
            }
        }
        return str;
    }

    private String toStringImpl(boolean jvmStyle) {
        StringBuilder builder = new StringBuilder();
        if (typeVariable != null) {
            builder.append('<');
        }
        if (packageName != null && !packageName.isEmpty()) {
            builder.append(packageName);
            builder.append('.');
        }
        boolean addDot = false;
        for (String simpleName : simpleNames) {
            if (addDot) {
                builder.append(jvmStyle ? '$' : '.');
            } else {
                addDot = true;
            }
            builder.append(simpleName);
        }
        if (typeVariable != null) {
            builder.append("::").append(typeVariable).append('>');
        }
        return builder.toString();
    }

    public static TypeName parse(String value) {
        String typeVariable;
        if (value.startsWith("<")) {
            int scopeIndex = value.lastIndexOf("::");
            typeVariable = value.substring(scopeIndex + 2, value.length() - 1);
            value = value.substring(1, scopeIndex);
        } else {
            typeVariable = null;
        }

        String packageName;
        int lastDotIndex = value.lastIndexOf('.');
        if (lastDotIndex != -1) {
            packageName = value.substring(0, lastDotIndex);
            value = value.substring(lastDotIndex + 1);
        } else {
            packageName = null;
        }

        List<String> simpleNames = Collections.unmodifiableList(Arrays.asList(DOLLAR_PATTERN.split(value)));

        return new TypeName(packageName, simpleNames, typeVariable);
    }

    static class SerializerV2 extends com.fasterxml.jackson.databind.JsonSerializer<TypeName> {
        @Override
        public void serialize(TypeName typeName,
                              com.fasterxml.jackson.core.JsonGenerator gen,
                              com.fasterxml.jackson.databind.SerializerProvider provider) throws IOException {
            gen.writeString(typeName.toString(true));
        }
    }

    static class DeserializerV2 extends com.fasterxml.jackson.databind.JsonDeserializer<TypeName> {
        @Override
        public TypeName deserialize(com.fasterxml.jackson.core.JsonParser jp,
                                    com.fasterxml.jackson.databind.DeserializationContext ctx) throws IOException {
            String value = jp.getValueAsString();
            return TypeName.parse(value);
        }
    }

    static class SerializerV3 extends tools.jackson.databind.ValueSerializer<TypeName> {
        @Override
        public void serialize(TypeName typeName,
                              tools.jackson.core.JsonGenerator gen,
                              tools.jackson.databind.SerializationContext ctx) {
            gen.writeString(typeName.toString(true));
        }
    }

    static class DeserializerV3 extends tools.jackson.databind.ValueDeserializer<TypeName> {
        @Override
        public TypeName deserialize(tools.jackson.core.JsonParser jp,
                                    tools.jackson.databind.DeserializationContext ctx) {
            String value = jp.getValueAsString();
            return TypeName.parse(value);
        }
    }
}
