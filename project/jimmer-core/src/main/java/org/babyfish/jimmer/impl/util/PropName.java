package org.babyfish.jimmer.impl.util;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class PropName {

    private final String value;

    private final boolean recordStyle;

    private PropName(String value, boolean recordStyle) {
        this.value = value;
        this.recordStyle = recordStyle;
    }

    public String getText() {
        return value;
    }

    public boolean isRecordStyle() {
        return recordStyle;
    }

    public static PropName fromBeanGetter(Method getter) {
        Class<?> returnType = getter.getReturnType();
        if (returnType == void.class || Modifier.isStatic(getter.getModifiers()) || getter.getParameters().length != 0) {
            return null;
        }
        return fromBeanGetterName(getter.getName(), returnType == boolean.class);
    }

    public static PropName fromBeanGetterName(String getterName, boolean isBoolean) {
        if (isBoolean && getterName.startsWith("is") && getterName.length() > 2) {
            return new PropName(lowercaseHead(getterName.substring(2)), false);
        }
        if (getterName.startsWith("get") && getterName.length() > 3) {
            return new PropName(lowercaseHead(getterName.substring(3)), false);
        }
        return new PropName(getterName, true);
    }

    private static String lowercaseHead(String name) {
        char[] arr = name.toCharArray();
        for (int i = 0; i < arr.length; i++) {
            char c = arr[i];
            if (Character.isUpperCase(c)) {
                arr[i] = Character.toLowerCase(c);
            } else {
                break;
            }
        }
        return new String(arr);
    }
}
