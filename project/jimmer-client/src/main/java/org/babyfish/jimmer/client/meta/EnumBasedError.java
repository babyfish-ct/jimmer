package org.babyfish.jimmer.client.meta;

import java.util.Map;

public class EnumBasedError {

    private final Enum<?> rawError;

    private final Map<String, Field> fields;

    public EnumBasedError(Enum<?> rawError, Map<String, Field> fields) {
        this.rawError = rawError;
        this.fields = fields;
    }

    public Enum<?> getRawError() {
        return rawError;
    }

    public Map<String, Field> getFields() {
        return fields;
    }

    @Override
    public String toString() {
        return "Error{" +
                "rawError=" + rawError +
                ", fields=" + fields +
                '}';
    }

    public static class Field {

        private final String name;

        private final Type type;

        public Field(String name, Type type) {
            this.name = name;
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public Type getType() {
            return type;
        }

        @Override
        public String toString() {
            return "Field{" +
                    "name='" + name + '\'' +
                    ", type=" + type +
                    '}';
        }
    }
}
