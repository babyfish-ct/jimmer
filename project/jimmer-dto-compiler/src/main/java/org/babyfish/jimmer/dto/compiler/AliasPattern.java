package org.babyfish.jimmer.dto.compiler;

import org.antlr.v4.runtime.Token;

class AliasPattern {

    private final boolean isPrefix;

    private final boolean isSuffix;

    private final String original;

    private final String replacement;

    public AliasPattern(DtoParser.AliasPatternContext pattern) {
        if (pattern.prefix != null && pattern.suffix != null) {
            throw new DtoAstException(
                    pattern.suffix.getLine(),
                    "The `^` and `$` cannot appear at the same time"
            );
        }
        if (pattern.original == null && pattern.replacement == null) {
            throw new DtoAstException(
                    pattern.translator.getLine(),
                    "There is no identifier to the left or right of the converter '->'"
            );
        }
        if (pattern.prefix == null && pattern.original == null && pattern.suffix == null) {
            throw new DtoAstException(
                    pattern.translator.getLine(),
                    "There is nothing to the left of the converter '->', which is not allowed"
            );
        }
        this.isPrefix = pattern.prefix != null;
        this.isSuffix = pattern.suffix != null;
        this.original = pattern.original != null ? pattern.original.getText() : null;
        this.replacement = pattern.replacement != null ? pattern.replacement.getText() : null;
    }

    public String alias(Token token) {
        return alias(token.getText(), token.getLine());
    }

    public String alias(String text, int line) {
        if (original == null) {
            if (isPrefix) {
                return join(replacement, text);
            }
            if (isSuffix) {
                return join(text, replacement);
            }
            throw new AssertionError("Internal bug of DTO compiler");
        }
        if (isPrefix) {
            if (text.startsWith(original)) {
                String right = text.substring(original.length());
                return replacement != null ? join(replacement, right) : right;
            }
            if (line == 0) {
                return text;
            }
            throw new DtoAstException(
                    line,
                    "The property \"" +
                            text +
                            "\" does not match the alias pattern \"" +
                            this +
                            "\", it must start with \"" +
                            original +
                            "\""
            );
        }
        if (isSuffix) {
            if (text.endsWith(original)) {
                String left = text.substring(0, text.length() - original.length());
                return replacement != null ? join(left, replacement) : left;
            }
            if (line == 0) {
                return text;
            }
            throw new DtoAstException(
                    line,
                    "The property \"" +
                            text +
                            "\" does not match the alias pattern \"" +
                            this +
                            "\", it must end with \"" +
                            original +
                            "\""
            );
        }
        int index = text.indexOf(original);
        if (index != -1) {
            String left = text.substring(0, index);
            String right = text.substring(index + original.length());
            return replacement != null ?
                    join(left, replacement, right) :
                    join(left, right);
        }
        if (line == 0) {
            return text;
        }
        throw new DtoAstException(
                line,
                "The property \"" +
                        text +
                        "\" does not match the alias pattern \"" +
                        this +
                        "\", it must contains \"" +
                        original +
                        "\""
        );
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (isPrefix) {
            builder.append('^');
        }
        if (original != null) {
            builder.append(original);
        }
        if (isSuffix) {
            builder.append('$');
        }
        builder.append(" -> ");
        if (replacement != null) {
            builder.append(replacement);
        }
        return builder.toString();
    }

    static String join(String ... parts) {
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
}
