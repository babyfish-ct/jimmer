package org.babyfish.jimmer.sql.meta;

import java.util.*;
import java.util.function.Function;

public abstract class SqlTemplate {

    final List<Object> parts;

    private final int charCount;

    private final Map<Placeholder, Integer> placeholderCountMap;

    protected SqlTemplate(List<Object> parts) {
        this.parts = parts;
        this.charCount = parts
                .stream()
                .mapToInt(it -> it instanceof String ? ((String)it).length() : 0)
                .sum();
        Map<Placeholder, Integer> map = new HashMap<>();
        for (Object o : parts) {
            if (o instanceof Placeholder) {
                Placeholder placeholder = (Placeholder)o;
                Integer count = map.get(placeholder);
                map.put(placeholder, count != null ? count + 1 : 1);
            }
        }
        this.placeholderCountMap = map;
    }

    protected static <T extends SqlTemplate> T create(
            String sql,
            Collection<Placeholder> placeholders,
            Function<List<Object>, T> constructor
    ) {
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
            if (c == '%') {
                boolean foundPlaceHolder = false;
                for (Placeholder placeHolder : placeholders) {
                    int holderLen = placeHolder.name.length();
                    if (i + holderLen + 1 <= size) {
                        boolean expectedEnd;
                        if (i + holderLen + 1 < size) {
                            char end = sql.charAt(i + holderLen + 1);
                            expectedEnd = end != '_' && end != '$' && !Character.isLetter(end) && !Character.isDigit(c);
                        } else {
                            expectedEnd = true;
                        }
                        if (expectedEnd) {
                            String word = sql.substring(i + 1, i + holderLen + 1);
                            if (word.equals(placeHolder.name)) {
                                String part = partBuilder.toString();
                                if (!part.isEmpty()) {
                                    parts.add(part);
                                    partBuilder = new StringBuilder();
                                }
                                parts.add(placeHolder);
                                i += holderLen;
                                foundPlaceHolder = true;
                                break;
                            }
                        }
                    }
                }
                if (foundPlaceHolder) {
                    continue;
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
                    if (parenthesesDepth == 0) {
                        throw new IllegalArgumentException(
                                "Unexpected character '" +
                                        c +
                                        "' the formula sql \"" +
                                        sql +
                                        "\""
                        );
                    }
                    break;
            }
            partBuilder.append(c);
        }
        String part = partBuilder.toString();
        if (!part.isEmpty()) {
            parts.add(part);
        }
        T template = constructor.apply(parts);
        SqlTemplate sqlTemplate = template;
        for (Placeholder placeholder : placeholders) {
            if (!sqlTemplate.placeholderCountMap.containsKey(placeholder)) {
                throw new IllegalArgumentException(
                        "The placeholder \"%" + placeholder.name + "\" is missing"
                );
            }
        }
        return template;
    }

    public String toSql(Map<Placeholder, String> placeHolderValues) {
        int capacity = charCount;
        for (Map.Entry<Placeholder, Integer> e : placeholderCountMap.entrySet()) {
            String value = placeHolderValues.get(e.getKey());
            if (value == null || value.isEmpty()) {
                throw new IllegalArgumentException(
                        "The value of placeholder \"%" + e.getKey() + "\" is not specified"
                );
            }
            capacity += value.length() * e.getValue();
        }
        StringBuilder builder = new StringBuilder(capacity);
        for (Object part : parts) {
            if (part instanceof String) {
                builder.append(part);
            } else {
                builder.append(placeHolderValues.get((Placeholder) part));
            }
        }
        return builder.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SqlTemplate that = (SqlTemplate) o;
        return charCount == that.charCount && parts.equals(that.parts) && placeholderCountMap.equals(that.placeholderCountMap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parts, charCount, placeholderCountMap);
    }

    protected static class Placeholder {

        final String name;

        public Placeholder(String name) {
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Placeholder)) return false;
            Placeholder that = (Placeholder) o;
            return name.equals(that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }

        @Override
        public String toString() {
            return "PlaceHolder{" +
                    "name='" + name + '\'' +
                    '}';
        }
    }
}
