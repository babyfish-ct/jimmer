package org.babyfish.jimmer.meta.impl;

import org.babyfish.jimmer.sql.meta.MetaStringResolver;
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

    public static String resolveMetaString(String name, MetaStringResolver resolver) {
        String resolved = resolver.resolve(name);
        if (resolved == null) {
            return name;
        }
        if (resolved.isEmpty() && !name.isEmpty()) {
            throw new IllegalStateException(
                    "Illegal class \"" +
                            resolver.getClass().getName() +
                            "\" which implements \"" +
                            MetaStringResolver.class.getName() +
                            "\", it method `resolve` cannot return empty string"
            );
        }
        return resolved;
    }
}
