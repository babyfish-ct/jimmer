package org.babyfish.jimmer.meta.impl;

import org.jetbrains.annotations.Nullable;

public class ViewUtils {

    private ViewUtils() {}

    @Nullable
    public static String defaultBasePropName(boolean isList, String name) {
        if (!isList && name.length() > 2 && !Character.isUpperCase(name.charAt(name.length() - 3)) && name.endsWith("Id")) {
            return name.substring(0, name.length() - 2);
        }
        return null;
    }
}
