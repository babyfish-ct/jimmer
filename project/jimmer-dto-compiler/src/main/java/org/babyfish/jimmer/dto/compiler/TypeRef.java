package org.babyfish.jimmer.dto.compiler;

import java.util.Collections;
import java.util.List;

public class TypeRef {

    private final String typeName;

    private final List<Argument> arguments;

    private final boolean isNullable;

    public TypeRef(String typeName, List<Argument> arguments, boolean isNullable) {
        this.typeName = typeName;
        this.arguments = arguments != null && !arguments.isEmpty() ?
                Collections.unmodifiableList(arguments) :
                Collections.emptyList();
        this.isNullable = isNullable;
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
        public String toString() {
            if (in) {
                return "in " + typeRef;
            }
            if (out) {
                return "out " + typeRef;
            }
            return typeRef.toString();
        }
    }
}
