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
                    prevUpper = true;
                } else {
                    prevUpper = false;
                }
                builder.append(c);
            } else {
                builder.append(Character.toUpperCase(c));
            }
        }
        return builder.toString();
    }
}
