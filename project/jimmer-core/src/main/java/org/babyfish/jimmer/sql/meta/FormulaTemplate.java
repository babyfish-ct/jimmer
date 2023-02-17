package org.babyfish.jimmer.sql.meta;

import org.babyfish.jimmer.meta.ModelException;

import java.util.ArrayList;
import java.util.List;

public class FormulaTemplate {

    private static final Object ALIAS_PLACEHOLDER = new Object();

    private final List<Object> parts;

    private final int charCount;

    private final int aliasCount;

    private FormulaTemplate(List<Object> parts) {
        this.parts = parts;
        this.charCount = parts
                .stream()
                .mapToInt(it -> it instanceof String ? ((String)it).length() : 0)
                .sum();
        this.aliasCount = (int)parts.stream().filter(it -> it == ALIAS_PLACEHOLDER).count();
    }

    public static FormulaTemplate of(String sql) {
        if (sql.isEmpty()) {
            throw new IllegalArgumentException("Empty sql");
        }
        List<Object> parts = new ArrayList<>();
        StringBuilder partBuilder = new StringBuilder();
        boolean inStr = false;
        int parenthesesDepth = 0;
        int size = sql.length();
        for (int i = 0; i < size; i ++) {
            char c = sql.charAt(i);
            if (c == '\'') {
                inStr = !inStr;
            }
            if (inStr) {
                partBuilder.append(c);
                continue;
            }
            if (c == '%' && i + 6 <= size) {
                boolean expectedEnd;
                if (i + 6 < size) {
                    char end = sql.charAt(i + 6);
                    expectedEnd = end != '_' && end != '$' && !Character.isLetter(end) && !Character.isDigit(c);
                } else {
                    expectedEnd = true;
                }
                if (expectedEnd) {
                    String word = sql.substring(i, i + 6);
                    if (word.equals("%alias")) {
                        String part = partBuilder.toString();
                        if (!part.isEmpty()) {
                            parts.add(part);
                            partBuilder = new StringBuilder();
                        }
                        parts.add(ALIAS_PLACEHOLDER);
                        i += 5;
                        continue;
                    }
                }
            }
            switch (c) {
                case '(':
                    parenthesesDepth++;
                    break;
                case ')':
                    parenthesesDepth--;
                    break;
                case ',':
                case ' ':
                    if (parenthesesDepth == 0) {
                        throw new ModelException(
                                "Unexpected character '" +
                                        c +
                                        "' the formula sql \"" +
                                        sql +
                                        "\""
                        );
                    }
            }
            partBuilder.append(c);
        }
        String part = partBuilder.toString();
        if (!part.isEmpty()) {
            parts.add(part);
        }
        if (parts.size() == 1) {
            throw new IllegalArgumentException("No \"%alias\"");
        }
        return new FormulaTemplate(parts);
    }

    public String toSql(String alias) {
        StringBuilder builder = new StringBuilder(charCount + aliasCount * alias.length());
        for (Object part : parts) {
            if (part instanceof String) {
                builder.append(part);
            } else {
                builder.append(alias);
            }
        }
        return builder.toString();
    }
}
