package org.babyfish.jimmer.json;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

/** A JSON value. A missing child returns null, while an explicit JSON null is a node. */
public interface Node {

    enum Kind { NULL, BOOLEAN, NUMBER, STRING, ARRAY, OBJECT }

    Kind kind();

    int size();

    Node get(int index);

    Node get(String fieldName);

    Iterator<Map.Entry<String, Node>> fieldsIterator();

    default boolean isNull() {
        return kind() == Kind.NULL;
    }

    /** Returns a Java scalar, or a tree of maps and lists. */
    Object toJavaValue();

    default boolean canCastTo(Class<?> type) {
        return type == String.class || type == UUID.class || type == Boolean.class || type == boolean.class ||
                type == Character.class || type == char.class || type == Byte.class || type == byte.class ||
                type == Short.class || type == short.class || type == Integer.class || type == int.class ||
                type == Long.class || type == long.class || type == Float.class || type == float.class ||
                type == Double.class || type == double.class || type == BigInteger.class || type == BigDecimal.class;
    }

    @SuppressWarnings("unchecked")
    default <T> T castTo(Class<T> type) {
        Object value = toJavaValue();
        if (value == null || type.isInstance(value)) {
            return (T) value;
        }
        String text = value.toString();
        if (type == String.class) {
            return (T) text;
        }
        if (type == UUID.class) {
            return (T) UUID.fromString(text);
        }
        if (type == boolean.class || type == Boolean.class) {
            return (T) Boolean.valueOf("true".equalsIgnoreCase(text) || "1".equals(text));
        }
        if (type == char.class || type == Character.class) {
            if (text.length() != 1) {
                throw new IllegalArgumentException("Expected a single character: " + text);
            }
            return (T) Character.valueOf(text.charAt(0));
        }
        BigDecimal number = new BigDecimal(text);
        if (type == BigDecimal.class) return (T) number;
        if (type == BigInteger.class) return (T) number.toBigIntegerExact();
        if (type == long.class || type == Long.class) return (T) Long.valueOf(number.longValueExact());
        if (type == int.class || type == Integer.class) return (T) Integer.valueOf(number.intValueExact());
        if (type == short.class || type == Short.class) return (T) Short.valueOf(number.shortValueExact());
        if (type == byte.class || type == Byte.class) return (T) Byte.valueOf(number.byteValueExact());
        if (type == double.class || type == Double.class) return (T) Double.valueOf(number.doubleValue());
        if (type == float.class || type == Float.class) return (T) Float.valueOf(number.floatValue());
        throw new IllegalArgumentException("Cannot cast JSON value to " + type.getName());
    }

    default boolean contentEquals(Node other) {
        if (other == null || kind() != other.kind() || size() != other.size()) {
            return false;
        }
        switch (kind()) {
            case OBJECT:
                Iterator<Map.Entry<String, Node>> fields = fieldsIterator();
                while (fields.hasNext()) {
                    Map.Entry<String, Node> field = fields.next();
                    if (!field.getValue().contentEquals(other.get(field.getKey()))) {
                        return false;
                    }
                }
                return true;
            case ARRAY:
                for (int i = 0; i < size(); i++) {
                    if (!get(i).contentEquals(other.get(i))) {
                        return false;
                    }
                }
                return true;
            case NUMBER:
                return castTo(BigDecimal.class).compareTo(other.castTo(BigDecimal.class)) == 0;
            default:
                return Objects.equals(toJavaValue(), other.toJavaValue());
        }
    }

    default int contentHashCode() {
        switch (kind()) {
            case OBJECT:
                int objectHash = 0;
                Iterator<Map.Entry<String, Node>> fields = fieldsIterator();
                while (fields.hasNext()) {
                    Map.Entry<String, Node> field = fields.next();
                    objectHash += field.getKey().hashCode() ^ field.getValue().contentHashCode();
                }
                return objectHash;
            case ARRAY:
                int arrayHash = 1;
                for (int i = 0; i < size(); i++) {
                    arrayHash = 31 * arrayHash + get(i).contentHashCode();
                }
                return arrayHash;
            case NUMBER:
                return castTo(BigDecimal.class).stripTrailingZeros().hashCode();
            default:
                return Objects.hashCode(toJavaValue());
        }
    }

    default <T> T convertTo(Type targetType, JsonConverter converter) throws Exception {
        return converter.convert(toJavaValue(), targetType);
    }

    default <T> T convertTo(Class<T> targetType, JsonConverter converter) throws Exception {
        return convertTo((Type) targetType, converter);
    }
}
