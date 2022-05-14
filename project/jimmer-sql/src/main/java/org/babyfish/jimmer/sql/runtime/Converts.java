package org.babyfish.jimmer.sql.runtime;

import java.math.BigDecimal;
import java.math.BigInteger;

public class Converts {

    private Converts() {}

    public static Object tryConvert(Object value, Class<?> expectedType) {
        if (value == null || value.getClass() == expectedType) {
            return value;
        }
        if (value instanceof Number) {
            Number num = (Number) value;
            if (expectedType == boolean.class || expectedType == Boolean.class) {
                return num.intValue() != 0;
            }
            if (expectedType == byte.class || expectedType == Byte.class) {
                return num.byteValue();
            }
            if (expectedType == short.class || expectedType == Short.class) {
                return num.shortValue();
            }
            if (expectedType == int.class || expectedType == Integer.class) {
                return num.intValue();
            }
            if (expectedType == long.class || expectedType == Long.class) {
                return num.longValue();
            }
            if (expectedType == float.class || expectedType == Float.class) {
                return num.floatValue();
            }
            if (expectedType == double.class || expectedType == Double.class) {
                return num.doubleValue();
            }
            if (expectedType == BigInteger.class) {
                if (value instanceof BigDecimal) {
                    return ((BigDecimal)value).toBigInteger();
                }
                return BigInteger.valueOf(((Number) value).longValue());
            }
            if (expectedType == BigDecimal.class) {
                if (value instanceof BigInteger) {
                    return ((BigInteger)value).toString();
                }
                return BigDecimal.valueOf(num.doubleValue());
            }
        }
        return null;
    }
}
