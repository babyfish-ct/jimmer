package org.babyfish.jimmer.impl.util;

public class StringUtil {

    public static String snake(String text, SnakeCase snakeCase) {
        StringBuilder builder = new StringBuilder();
        int size = text.length();
        boolean isPrevLowerCase = false;
        for (int i = 0; i < size; i++) {
            char c = text.charAt(i);
            boolean isLowerCase = Character.isLowerCase(c);
            if (isPrevLowerCase && !isLowerCase) {
                builder.append('_');
            }
            isPrevLowerCase = isLowerCase;
            switch (snakeCase) {
                case UPPER:
                    builder.append(Character.toUpperCase(c));
                    break;
                case LOWER:
                    builder.append(Character.toLowerCase(c));
                    break;
                default:
                    builder.append(c);
                    break;
            }
        }
        return builder.toString();
    }

    public enum SnakeCase {
        ORIGINAL,
        LOWER,
        UPPER
    }
}
