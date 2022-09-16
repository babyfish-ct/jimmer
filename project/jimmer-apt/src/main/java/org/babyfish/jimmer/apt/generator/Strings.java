package org.babyfish.jimmer.apt.generator;

public class Strings {

    private Strings() {}

    public static String upper(String text) {
        boolean prevUpper = true;
        StringBuilder builder = new StringBuilder();
        int size = text.length();
        for (int i = 0; i < size; i++) {
            char c = text.charAt(i);
            boolean upper = Character.isUpperCase(c);
            if (upper) {
                if (!prevUpper) {
                    builder.append('_');
                }
                builder.append(c);
            } else {
                builder.append(Character.toUpperCase(c));
            }
            prevUpper = upper;
        }
        return builder.toString();
    }
}
