package org.babyfish.jimmer.meta.impl;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.jetbrains.annotations.Nullable;

public class Utils {

    private Utils() {}

    @Nullable
    public static String defaultViewBasePropName(boolean isList, String name) {
        if (!isList && name.length() > 2 && !Character.isUpperCase(name.charAt(name.length() - 3)) && name.endsWith("Id")) {
            return name.substring(0, name.length() - 2);
        }
        return null;
    }
}
