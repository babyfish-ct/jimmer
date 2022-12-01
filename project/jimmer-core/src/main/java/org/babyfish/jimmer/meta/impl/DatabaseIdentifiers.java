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

    public static String comparableIdentifier(String identifier) {
        boolean cut = false;
        if (identifier.startsWith("`") && identifier.endsWith("`") && identifier.length() > 2) {
            cut = true;
        } else if (identifier.startsWith("\"") && identifier.endsWith("\"") && identifier.length() > 2) {
            cut = true;
        } else if (identifier.startsWith("[") && identifier.endsWith("]")) {
            cut = true;
        }
        return (cut ? identifier.substring(1, identifier.length() - 1) : identifier).toUpperCase();
    }
}
