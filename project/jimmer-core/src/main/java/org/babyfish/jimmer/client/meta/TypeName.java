package org.babyfish.jimmer.client.meta;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

@JsonSerialize(using = TypeName.Serializer.class)
@JsonDeserialize(using = TypeName.Deserializer.class)
public final class TypeName implements Comparable<TypeName> {

    public static final TypeName VOID = new TypeName(null, "void");

    public static final TypeName BOOLEAN = new TypeName(null, "boolean");

    public static final TypeName CHAR = new TypeName(null, "char");

    public static final TypeName BYTE = new TypeName(null, "byte");

    public static final TypeName SHORT = new TypeName(null, "short");

    public static final TypeName INT = new TypeName(null, "int");

    public static final TypeName LONG = new TypeName(null, "long");

    public static final TypeName FLOAT = new TypeName(null, "float");

    public static final TypeName DOUBLE = new TypeName(null, "double");

    public static final TypeName OBJECT = new TypeName("java.lang", "Object");

    public static final TypeName STRING = new TypeName("java.lang", "String");

    public static final TypeName ITERABLE = new TypeName("java.util", "Iterable");

    public static final TypeName COLLECTION = new TypeName("java.util", "Collection");

    public static final TypeName LIST = new TypeName("java.util", "List");

    public static final TypeName SET = new TypeName("java.util", "Set");

    public static final TypeName MAP = new TypeName("java.util", "Map");

    private static final Pattern DOT_PATTERN = Pattern.compile("\\.");

    @Nullable
    private final String packageName;

    private final List<String> simpleNames;

    @Nullable
    private final String typeVariable;

    private String javaString;

    private String clientString;

    public TypeName(@Nullable String packageName, String simpleName) {
        this(packageName, Collections.singletonList(simpleName), null);
    }

    public TypeName(@Nullable String packageName, List<String> simpleNames) {
        this(packageName, simpleNames, null);
    }

    public TypeName(@Nullable String packageName, List<String> simpleNames, @Nullable String typeVariable) {
        this.packageName = packageName != null && !packageName.isEmpty() ? packageName : null;
        this.simpleNames = Collections.unmodifiableList(simpleNames);
        this.typeVariable = typeVariable;
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

    public String toString(boolean clientStyle) {
        String str;
        if (clientStyle) {
            str = clientString;
            if (str == null) {
                clientString = str = toStringImpl(true);
            }
        } else {
            str = javaString;
            if (str == null) {
                javaString = str = toStringImpl(false);
            }
        }
        return str;
    }

    private String toStringImpl(boolean clientStyle) {
        StringBuilder builder = new StringBuilder();
        if (typeVariable != null) {
            builder.append('<');
        }
        if (packageName != null && !packageName.isEmpty()) {
            builder.append(packageName);
            if (clientStyle) {
                builder.append('/');
            } else {
                builder.append('.');
            }
        }
        boolean addDot = false;
        for (String simpleName : simpleNames) {
            if (addDot) {
                builder.append('.');
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
        int slashIndex = value.indexOf('/');
        if (slashIndex != -1) {
            packageName = value.substring(0, slashIndex);
            value = value.substring(slashIndex + 1);
        } else {
            packageName = null;
        }

        List<String> simpleNames = Collections.unmodifiableList(Arrays.asList(DOT_PATTERN.split(value)));

        return new TypeName(packageName, simpleNames, typeVariable);
    }

    public static class Serializer extends JsonSerializer<TypeName> {

        @Override
        public void serialize(TypeName typeName, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeString(typeName.toString(true));
        }
    }

    public static class Deserializer extends JsonDeserializer<TypeName> {

        @Override
        public TypeName deserialize(JsonParser jp, DeserializationContext ctx) throws IOException, JacksonException {
            String value = jp.getValueAsString();
            return TypeName.parse(value);
        }
    }
}
