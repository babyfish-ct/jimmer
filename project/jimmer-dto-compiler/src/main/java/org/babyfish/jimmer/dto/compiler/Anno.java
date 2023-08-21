package org.babyfish.jimmer.dto.compiler;

import java.util.List;
import java.util.Map;

public class Anno {

    private final String qualifiedName;

    private final Map<String, Value> valueMap;

    public Anno(String qualifiedName, Map<String, Value> valueMap) {
        this.qualifiedName = qualifiedName;
        this.valueMap = valueMap;
    }

    public String getQualifiedName() {
        return qualifiedName;
    }

    public Map<String, Value> getValueMap() {
        return valueMap;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append('@').append(qualifiedName);
        if (!valueMap.isEmpty()) {
            builder.append('(');
            boolean addComma = false;
            for (Map.Entry<String, Value> e : valueMap.entrySet()) {
                if (addComma) {
                    builder.append(", ");
                } else {
                    addComma = true;
                }
                builder.append(e.getKey()).append(" = ").append(e.getValue());
            }
            builder.append(')');
        }
        return builder.toString();
    }

    public static abstract class Value {}

    public static class ArrayValue extends Value {

        public final List<Value> elements;

        public ArrayValue(List<Value> elements) {
            this.elements = elements;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append('[');
            boolean addComma = false;
            for (Value element : elements) {
                if (addComma) {
                    builder.append(", ");
                } else {
                    addComma = true;
                }
                builder.append(element);
            }
            builder.append(']');
            return builder.toString();
        }
    }

    public static class AnnoValue extends Value {

        public final Anno anno;

        public AnnoValue(Anno anno) {
            this.anno = anno;
        }

        @Override
        public String toString() {
            return anno.toString();
        }
    }

    public static class EnumValue extends Value {

        public final String qualifiedName;

        public final String constant;

        public EnumValue(String qualifiedName, String constant) {
            this.qualifiedName = qualifiedName;
            this.constant = constant;
        }

        @Override
        public String toString() {
            return qualifiedName + '.' + constant;
        }
    }

    public static class LiteralValue extends Value {

        public final String value;

        public LiteralValue(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }
}
