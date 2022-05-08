package org.babyfish.jimmer.meta;

class Utils {

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
}
