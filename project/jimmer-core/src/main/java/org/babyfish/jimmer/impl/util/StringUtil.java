package org.babyfish.jimmer.impl.util;

import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

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

    public static String typeName(String ... parts) {
        StringBuilder builder = new StringBuilder();
        boolean prevPartEndsWithLower = true;
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
            boolean isLowerCase = Character.isLowerCase(c) || Character.isDigit(c);
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

    @Nullable
    public static String propName(String method, boolean isBoolean) {
        if (method.length() > 3 && !Character.isLowerCase(method.charAt(3)) && method.startsWith("get")) {
            return identifier(method.substring(3));
        }
        if (isBoolean && method.length() > 2 && method.startsWith("is") && !Character.isLowerCase(method.charAt(2))) {
            return identifier(method.substring(2));
        }
        return null;
    }

    public static String removeSuffixes(String value, String... suffixes) {
        Arrays.sort(suffixes, (a, b) -> b.length() - a.length());
        for (String suffix : suffixes) {
            if (suffix != null && !suffix.isEmpty() && value.endsWith(suffix)) {
                return value.substring(0, value.length() - suffix.length());
            }
        }
        return null;
    }
}
