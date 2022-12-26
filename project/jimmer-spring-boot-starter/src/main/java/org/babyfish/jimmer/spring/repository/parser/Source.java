package org.babyfish.jimmer.spring.repository.parser;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class Source {

    private static final Comparator<String> LENGTH_DESC_COMPARATOR =
            Comparator.comparing(String::length).reversed();

    private final String raw;

    private final int from;

    private final int to;

    public Source(String raw) {
        this(raw, 0, raw.length());
    }

    private Source(String raw, int from, int to) {
        this.raw = raw;
        this.from = from;
        this.to = to;
    }

    public boolean isEmpty() {
        return from == to;
    }

    public int length() {
        return to - from;
    }

    public int indexOf(String str) {
        int index = this.raw.indexOf(str, from);
        if (index == -1 || index + str.length() > to) {
            return -1;
        }
        return index - from;
    }

    public Source trimStart(String str) {
        int len = str.length();
        if (from + len > to) {
            return null;
        }
        for (int i = 0; i < len; i++) {
            if (raw.charAt(from + i) != str.charAt(i)) {
                return null;
            }
        }
        return new Source(raw, from + len, to);
    }

    public Source trimStart(String ... arr) {
        List<String> list = Arrays.asList(arr);
        list.sort(LENGTH_DESC_COMPARATOR);
        for (String str : list) {
            Source rest = trimStart(str);
            if (rest != null) {
                return rest;
            }
        }
        return null;
    }

    public Source trimEnd(String str) {
        int len = str.length();
        int from = to - len;
        if (from < this.from) {
            return null;
        }
        for (int i = 0; i < len; i++) {
            if (raw.charAt(from + i) != str.charAt(i)) {
                return null;
            }
        }
        return new Source(raw, this.from, to - len);
    }

    public Source trimEnd(String ... arr) {
        List<String> list = Arrays.asList(arr);
        list.sort(LENGTH_DESC_COMPARATOR);
        for (String str : list) {
            Source rest = trimEnd(str);
            if (rest != null) {
                return rest;
            }
        }
        return null;
    }

    public Source subSource(int from) {
        return subSource(from, this.to - this.from);
    }

    public Source subSource(int from, int to) {
        int len = this.to - this.from;
        if (from > to || from < 0 || to > len) {
            throw new IllegalArgumentException("Illegal from and to");
        }
        if (from == 0 && to == len) {
            return this;
        }
        return new Source(raw, this.from + from, this.from + to);
    }

    public char charAt(int index) {
        if (index < 0 || index > to - from) {
            throw new IndexOutOfBoundsException();
        }
        return raw.charAt(this.from + index);
    }

    public String asString() {
        return raw.substring(from, to);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        int len = raw.length();
        for (int i = 0; i < len; i++) {
            if (i == from) {
                builder.append('[');
            }
            if (i == to) {
                builder.append(']');
            }
            builder.append(raw.charAt(i));
        }
        if (to == len) {
            builder.append(']');
        }
        return builder.toString();
    }
}
