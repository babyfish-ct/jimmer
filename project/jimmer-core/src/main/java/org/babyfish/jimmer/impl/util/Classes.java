package org.babyfish.jimmer.impl.util;

public class Classes {

    public static Class<?> boxTypeOf(Class<?> type) {
        if (type.isPrimitive()) {
            if (type == boolean.class) {
                return Boolean.class;
            }
            if (type == char.class) {
                return Character.class;
            }
            if (type == byte.class) {
                return Byte.class;
            }
            if (type == short.class) {
                return Short.class;
            }
            if (type == int.class) {
                return Integer.class;
            }
            if (type == long.class) {
                return Long.class;
            }
            if (type == float.class) {
                return Float.class;
            }
            if (type == double.class) {
                return Double.class;
            }
        }
        return type;
    }

    public static Class<?> primitiveTypeOf(Class<?> type) {
        if (type == Boolean.class) {
            return boolean.class;
        }
        if (type == Character.class) {
            return char.class;
        }
        if (type == Byte.class) {
            return byte.class;
        }
        if (type == Short.class) {
            return short.class;
        }
        if (type == Integer.class) {
            return int.class;
        }
        if (type == Long.class) {
            return long.class;
        }
        if (type == Float.class) {
            return float.class;
        }
        if (type == Double.class) {
            return double.class;
        }
        return type;
    }

    public static boolean matches(Class<?> a, Class<?> b) {
        if (a == b) {
            return true;
        }
        return boxTypeOf(a) == boxTypeOf(b);
    }
}
