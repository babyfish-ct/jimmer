package org.babyfish.jimmer.dto.compiler;

import org.jetbrains.annotations.Nullable;

import java.util.*;

public class TypeRef {

    public static final String TN_BOOLEAN = "Boolean";

    public static final String TN_CHAR = "Char";

    public static final String TN_BYTE = "Byte";

    public static final String TN_SHORT = "Short";

    public static final String TN_INT = "Int";

    public static final String TN_LONG = "Long";

    public static final String TN_FLOAT = "Float";

    public static final String TN_DOUBLE = "Double";

    public static final String TN_ANY = "Any";

    public static final String TN_STRING = "String";

    public static final String TN_ARRAY = "Array";

    public static final String TN_ITERABLE = "Iterable";

    public static final String TN_MUTABLE_ITERABLE = "MutableIterable";

    public static final String TN_COLLECTION = "Collection";

    public static final String TN_MUTABLE_COLLECTION = "MutableCollection";

    public static final String TN_LIST = "List";

    public static final String TN_MUTABLE_LIST = "MutableList";

    public static final String TN_SET = "Set";

    public static final String TN_MUTABLE_SET = "MutableSet";

    public static final String TN_MAP = "Map";

    public static final String TN_MUTABLE_MAP = "MutableMap";

    public static final Set<String> PRIMITIVE_TNS;

    public static final Set<String> COLLECTION_TNS;

    public static final Set<String> TNS_WITH_DEFAULT_VALUE;

    private final String typeName;

    private final List<Argument> arguments;

    private final boolean isNullable;

    private final int line;

    private final int col;

    public TypeRef(String typeName, List<Argument> arguments, boolean isNullable, int line, int col) {
        this.typeName = typeName;
        this.arguments = arguments != null && !arguments.isEmpty() ?
                Collections.unmodifiableList(arguments) :
                Collections.emptyList();
        this.isNullable = isNullable;
        this.line = line;
        this.col = col;
    }

    public String getTypeName() {
        return typeName;
    }

    public List<Argument> getArguments() {
        return arguments;
    }

    public boolean isNullable() {
        return isNullable;
    }

    public int getLine() {
        return line;
    }

    public int getCol() {
        return col;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TypeRef typeRef = (TypeRef) o;
        return isNullable == typeRef.isNullable && Objects.equals(typeName, typeRef.typeName) && Objects.equals(arguments, typeRef.arguments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(typeName, arguments, isNullable);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(typeName);
        if (!arguments.isEmpty()) {
            builder.append('<');
            boolean addComma = false;
            for (Argument argument : arguments) {
                if (addComma) {
                    builder.append(", ");
                } else {
                    addComma = true;
                }
                builder.append(argument);
            }
            builder.append('>');
        }
        if (isNullable) {
            builder.append("?");
        }
        return builder.toString();
    }

    public static class Argument {

        private final TypeRef typeRef;

        private final boolean in;

        private final boolean out;

        public Argument(TypeRef typeRef, boolean in, boolean out) {
            this.typeRef = typeRef;
            this.in = in;
            this.out = out;
        }

        @Nullable
        public TypeRef getTypeRef() {
            return typeRef;
        }

        public boolean isIn() {
            return in;
        }

        public boolean isOut() {
            return out;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Argument argument = (Argument) o;
            return in == argument.in && out == argument.out && Objects.equals(typeRef, argument.typeRef);
        }

        @Override
        public int hashCode() {
            return Objects.hash(typeRef, in, out);
        }

        @Override
        public String toString() {
            String typeString = typeRef != null ? typeRef.toString() : "*";
            if (in) {
                return "in " + typeString;
            }
            if (out) {
                return "out " + typeString;
            }
            return typeString;
        }
    }

    static {
        Set<String> primitiveTypeNames = new LinkedHashSet<>();
        primitiveTypeNames.add(TN_BOOLEAN);
        primitiveTypeNames.add(TN_CHAR);
        primitiveTypeNames.add(TN_BYTE);
        primitiveTypeNames.add(TN_SHORT);
        primitiveTypeNames.add(TN_INT);
        primitiveTypeNames.add(TN_LONG);
        primitiveTypeNames.add(TN_FLOAT);
        primitiveTypeNames.add(TN_DOUBLE);

        Set<String> collectionTypeNames = new LinkedHashSet<>();
        collectionTypeNames.add(TN_ITERABLE);
        collectionTypeNames.add(TN_COLLECTION);
        collectionTypeNames.add(TN_SET);
        collectionTypeNames.add(TN_LIST);
        collectionTypeNames.add(TN_MAP);
        collectionTypeNames.add(TN_MUTABLE_ITERABLE);
        collectionTypeNames.add(TN_MUTABLE_COLLECTION);
        collectionTypeNames.add(TN_MUTABLE_SET);
        collectionTypeNames.add(TN_MUTABLE_LIST);
        collectionTypeNames.add(TN_MUTABLE_MAP);

        Set<String> typeNamesWithDefaultValue = new LinkedHashSet<>(primitiveTypeNames);
        typeNamesWithDefaultValue.add(TN_ANY);
        typeNamesWithDefaultValue.add(TN_STRING);
        typeNamesWithDefaultValue.add(TN_ARRAY);
        typeNamesWithDefaultValue.add(TN_ITERABLE);
        typeNamesWithDefaultValue.add(TN_MUTABLE_ITERABLE);
        typeNamesWithDefaultValue.add(TN_COLLECTION);
        typeNamesWithDefaultValue.add(TN_MUTABLE_COLLECTION);
        typeNamesWithDefaultValue.add(TN_LIST);
        typeNamesWithDefaultValue.add(TN_MUTABLE_LIST);
        typeNamesWithDefaultValue.add(TN_SET);
        typeNamesWithDefaultValue.add(TN_MUTABLE_SET);
        typeNamesWithDefaultValue.add(TN_MAP);
        typeNamesWithDefaultValue.add(TN_MUTABLE_MAP);

        PRIMITIVE_TNS = Collections.unmodifiableSet(primitiveTypeNames);
        COLLECTION_TNS = Collections.unmodifiableSet(collectionTypeNames);
        TNS_WITH_DEFAULT_VALUE = Collections.unmodifiableSet(typeNamesWithDefaultValue);
    }
}
