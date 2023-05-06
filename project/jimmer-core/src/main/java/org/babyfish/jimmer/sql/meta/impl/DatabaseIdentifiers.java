package org.babyfish.jimmer.sql.meta.impl;

public class DatabaseIdentifiers {

    private DatabaseIdentifiers() {}

    public static String comparableIdentifier(String identifier) {
        if (identifier == null) {
            return null;
        }
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
