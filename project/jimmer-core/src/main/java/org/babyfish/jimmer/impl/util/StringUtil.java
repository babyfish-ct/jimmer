package org.babyfish.jimmer.impl.util;

public class StringUtil {

    public static String identifier(String ... parts) {
        StringBuilder builder = new StringBuilder();
        boolean prevPartEndsWithLower = false;
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (prevPartEndsWithLower) {
                if (Character.isUpperCase(part.charAt(0))) {
                    builder.append(part);
                } else {
                    builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
                }
            } else {
                if (Character.isLowerCase(part.charAt(0))) {
                    builder.append(part);
                } else {
                    char[] arr = part.toCharArray();
                    for (int i = 0; i < arr.length; i++) {
                        if (Character.isLowerCase(arr[i])) {
                            break;
                        }
                        arr[i] = Character.toLowerCase(arr[i]);
                    }
                    builder.append(arr);
                }
            }
            prevPartEndsWithLower = Character.isLowerCase(part.charAt(part.length() - 1));
        }
        return builder.toString();
    }

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
