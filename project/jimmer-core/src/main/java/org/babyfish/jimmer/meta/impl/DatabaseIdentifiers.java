package org.babyfish.jimmer.meta.impl;

public class DatabaseIdentifiers {

    private DatabaseIdentifiers() {}

    public static String databaseIdentifier(String name) {
        StringBuilder builder = new StringBuilder();
        boolean prevUpper = true;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c)) {
                if (!prevUpper) {
                    builder.append('_');
                }
                builder.append(c);
                prevUpper = true;
            } else {
                builder.append(Character.toUpperCase(c));
                prevUpper = false;
            }
        }
        return builder.toString();
    }

    public static String standardColumnName(String columName) {
        boolean cut = false;
        if (columName.startsWith("`") && columName.endsWith("`") && columName.length() > 2) {
            cut = true;
        } else if (columName.startsWith("\"") && columName.endsWith("\"") && columName.length() > 2) {
            cut = true;
        } else if (columName.startsWith("[") && columName.endsWith("]")) {
            cut = true;
        }
        return (cut ? columName.substring(1, columName.length() - 1) : columName).toUpperCase();
    }
}
