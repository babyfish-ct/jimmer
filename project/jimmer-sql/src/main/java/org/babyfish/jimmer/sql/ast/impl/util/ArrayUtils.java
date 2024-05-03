package org.babyfish.jimmer.sql.ast.impl.util;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class ArrayUtils {

    private static final Boolean[] EMPTY_BOOLEAN_ARR = new Boolean[0];

    private static final Character[] EMPTY_CHARACTER_ARR = new Character[0];

    private static final Byte[] EMPTY_BYTE_ARR = new Byte[0];

    private static final Short[] EMPTY_SHORT_ARR = new Short[0];

    private static final Integer[] EMPTY_INTEGER_ARR = new Integer[0];

    private static final Long[] EMPTY_LONG_ARR = new Long[0];

    private static final Float[] EMPTY_FLOAT_ARR = new Float[0];

    private static final Double[] EMPTY_DOUBLE_ARR = new Double[0];

    private static final Map<Class<?>, Function<Object, Object[]>> TO_OBJECT_MAP;

    private ArrayUtils() {

    }

    public static Object[] toObject(Object arr) {
        if (arr == null || !arr.getClass().isArray()) {
            throw new IllegalArgumentException("The argument `arr` is not array");
        }
        Function<Object, Object[]> func = TO_OBJECT_MAP.get(arr.getClass().getComponentType());
        if (func != null) {
            return func.apply(arr);
        }
        return (Object[]) arr;
    }

    public static Boolean[] toObject(boolean[] arr) {
        if (arr == null) {
            return null;
        }
        int len = arr.length;
        if (len == 0) {
            return EMPTY_BOOLEAN_ARR;
        }
        Boolean[] objArr = new Boolean[len];
        for (int i = len - 1; i >= 0; --i) {
            objArr[i] = arr[i];
        }
        return objArr;
    }

    public static Character[] toObject(char[] arr) {
        if (arr == null) {
            return null;
        }
        int len = arr.length;
        if (len == 0) {
            return EMPTY_CHARACTER_ARR;
        }
        Character[] objArr = new Character[len];
        for (int i = len - 1; i >= 0; --i) {
            objArr[i] = arr[i];
        }
        return objArr;
    }

    public static Byte[] toObject(byte[] arr) {
        if (arr == null) {
            return null;
        }
        int len = arr.length;
        if (len == 0) {
            return EMPTY_BYTE_ARR;
        }
        Byte[] objArr = new Byte[len];
        for (int i = len - 1; i >= 0; --i) {
            objArr[i] = arr[i];
        }
        return objArr;
    }

    public static Short[] toObject(short[] arr) {
        if (arr == null) {
            return null;
        }
        int len = arr.length;
        if (len == 0) {
            return EMPTY_SHORT_ARR;
        }
        Short[] objArr = new Short[len];
        for (int i = len - 1; i >= 0; --i) {
            objArr[i] = arr[i];
        }
        return objArr;
    }

    public static Integer[] toObject(int[] arr) {
        if (arr == null) {
            return null;
        }
        int len = arr.length;
        if (len == 0) {
            return EMPTY_INTEGER_ARR;
        }
        Integer[] objArr = new Integer[len];
        for (int i = len - 1; i >= 0; --i) {
            objArr[i] = arr[i];
        }
        return objArr;
    }

    public static Long[] toObject(long[] arr) {
        if (arr == null) {
            return null;
        }
        int len = arr.length;
        if (len == 0) {
            return EMPTY_LONG_ARR;
        }
        Long[] objArr = new Long[len];
        for (int i = len - 1; i >= 0; --i) {
            objArr[i] = arr[i];
        }
        return objArr;
    }

    public static Float[] toObject(float[] arr) {
        if (arr == null) {
            return null;
        }
        int len = arr.length;
        if (len == 0) {
            return EMPTY_FLOAT_ARR;
        }
        Float[] objArr = new Float[len];
        for (int i = len - 1; i >= 0; --i) {
            objArr[i] = arr[i];
        }
        return objArr;
    }

    public static Double[] toObject(double[] arr) {
        if (arr == null) {
            return null;
        }
        int len = arr.length;
        if (len == 0) {
            return EMPTY_DOUBLE_ARR;
        }
        Double[] objArr = new Double[len];
        for (int i = len - 1; i >= 0; --i) {
            objArr[i] = arr[i];
        }
        return objArr;
    }

    static {
        Map<Class<?>, Function<Object, Object[]>> map = new HashMap<>();
        map.put(boolean.class, arr -> toObject((boolean[])arr));
        map.put(char.class, arr -> toObject((char[])arr));
        map.put(byte.class, arr -> toObject((byte[])arr));
        map.put(short.class, arr -> toObject((short[])arr));
        map.put(int.class, arr -> toObject((int[])arr));
        map.put(long.class, arr -> toObject((long[])arr));
        map.put(float.class, arr -> toObject((float[])arr));
        map.put(double.class, arr -> toObject((double[])arr));
        TO_OBJECT_MAP = map;
    }
}
