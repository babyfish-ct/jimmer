package org.babyfish.jimmer.sql.meta;

import org.babyfish.jimmer.jackson.codec.Node;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ModelException;
import org.babyfish.jimmer.sql.JoinTable;

import java.util.*;
import java.util.function.Function;

public class JoinTableFilterInfo {
    private final String columnName;

    private final Class<?> type;

    private final List<Object> values;

    private final Function<Node, Object> jsonNodeValueGetter;

    public JoinTableFilterInfo(String columnName, Class<?> type, List<Object> values) {
        if (!type.isPrimitive() && !String.class.equals(type)) {
            throw new IllegalArgumentException("type must be primitive or String");
        }
        this.columnName = columnName;
        this.type = type;
        this.values = values;
        this.jsonNodeValueGetter = boolean.class.equals(type) ?
                JoinTableFilterInfo::castToBoolean :
                n -> n.castTo(type);
    }

    public String getColumnName() {
        return columnName;
    }

    public Class<?> getType() {
        return type;
    }

    public List<Object> getValues() {
        return values;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JoinTableFilterInfo that = (JoinTableFilterInfo) o;

        if (!columnName.equals(that.columnName)) return false;
        if (!type.equals(that.type)) return false;
        return values.equals(that.values);
    }

    @Override
    public int hashCode() {
        int result = columnName.hashCode();
        result = 31 * result + type.hashCode();
        result = 31 * result + values.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "JoinTableFilterInfo{" +
                "columnName='" + columnName + '\'' +
                ", type=" + type +
                ", values=" + values +
                '}';
    }

    public static JoinTableFilterInfo of(ImmutableProp prop) {
        if (prop.getMappedBy() != null) {
            prop = prop.getMappedBy();
        }
        JoinTable joinTable = prop.getAnnotation(JoinTable.class);
        if (joinTable == null) {
            return null;
        }
        JoinTable.JoinTableFilter filter = joinTable.filter();
        if (filter == null) {
            return null;
        }
        if (filter.columnName().equals("<illegal-column-name>")) {
            return null;
        }
        if (filter.columnName().isEmpty()) {
            throw new ModelException(
                    "Illegal property \"" +
                            prop +
                            "\", the argument \"columnName\" of \"@" +
                            JoinTable.JoinTableFilter.class.getName() +
                            "\" cannot be empty string"
            );
        }
        Class<?> type = filter.type();
        if (!type.isPrimitive() && type != String.class) {
            if (filter.type() != String.class || filter.values().length != 0) {
                throw new ModelException(
                        "Illegal property \"" +
                                prop +
                                "\", the argument \"type\" of \"@" +
                                JoinTable.JoinTableFilter.class.getName() +
                                "\" must be primitive type of string"
                );
            }
        }
        String[] values = filter.values();
        if (values.length == 0) {
            throw new ModelException(
                    "Illegal property \"" +
                            prop +
                            "\", the argument \"values\" of \"@" +
                            JoinTable.JoinTableFilter.class.getName() +
                            "\" cannot be default value"
            );
        }
        Set<Object> parsedValues = new TreeSet<>();
        for (String value : values) {
            Object parsedValue = parseValue(value, type, prop);
            if (!parsedValues.add(parsedValue)) {
                throw new ModelException(
                        "Illegal property \"" +
                                prop +
                                "\", the elements of the argument \"values\" of \"@" +
                                JoinTable.JoinTableFilter.class.getName() +
                                "\" are not unique, the duplicated value is \"" +
                                parsedValue +
                                "\""
                );
            }
        }
        return new JoinTableFilterInfo(
                filter.columnName(),
                type,
                Collections.unmodifiableList(new ArrayList<>(parsedValues))
        );
    }

    private static Object parseValue(String value, Class<?> type, ImmutableProp prop) {
        if (value.equals("null")) {
            throw new ModelException(
                    "Illegal property \"" +
                            prop +
                            "\", the argument \"values\" of \"@" +
                            JoinTable.JoinTableFilter.class.getName() +
                            "\" cannot contains \"null\""
            );
        }
        if (type == String.class) {
            return value;
        }
        if (type == boolean.class) {
            return Boolean.parseBoolean(value);
        }
        if (type == char.class) {
            if (value.length() != 1) {
                throw new ModelException(
                        "Illegal property \"" +
                                prop +
                                "\", the argument \"values\" of \"@" +
                                JoinTable.JoinTableFilter.class.getName() +
                                "\" cannot contains \"" +
                                value +
                                "\" because its type is char"
                );
            }
            return value;
        }
        try {
            if (type == byte.class) {
                return Byte.parseByte(value);
            }
            if (type == short.class) {
                return Short.parseShort(value);
            }
            if (type == int.class) {
                return Integer.parseInt(value);
            }
            if (type == long.class) {
                return Long.parseLong(value);
            }
            if (type == float.class) {
                return Float.parseFloat(value);
            }
            if (type == double.class) {
                return Double.parseDouble(value);
            }
        } catch (NumberFormatException ex) {
            throw new ModelException(
                    "Illegal property \"" +
                            prop +
                            "\", the argument \"values\" of \"@" +
                            JoinTable.JoinTableFilter.class.getName() +
                            "\" cannot contains \"" +
                            value +
                            "\" which cannot be parsed to \"" +
                            type.getName() +
                            "\""
            );
        }
        throw new AssertionError("Internal bug: Illegal filtered column type");
    }

    public Object parse(Node node) {
        return jsonNodeValueGetter.apply(node);
    }

    private static boolean castToBoolean(Node node) {
        String text = node.castTo(String.class);
        if ("true".equals(text) || "yes".equals(text)) {
            return true;
        }
        return node.castTo(int.class) != 0;
    }
}
