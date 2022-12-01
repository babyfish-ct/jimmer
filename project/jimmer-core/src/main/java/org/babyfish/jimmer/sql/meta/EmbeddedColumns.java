package org.babyfish.jimmer.sql.meta;

import org.jetbrains.annotations.NotNull;

import java.util.*;

public class EmbeddedColumns implements ColumnDefinition {

    private static final String[] EMPTY_ARR = new String[0];

    private final Map<String, Partial> columnMap;

    private final String[] arr;

    public EmbeddedColumns(Map<String, List<String>> columnMap) {
        Map<String, Partial> map = new HashMap<>();
        for (Map.Entry<String, List<String>> e : columnMap.entrySet()) {
            String key = e.getKey();
            List<String> value = e.getValue();
            map.put(key, new Partial(key, value));
        }
        this.columnMap = map;
        this.arr = columnMap.get("").toArray(EMPTY_ARR);
    }

    public Partial partial(String path) {
        if (path == null) {
            path = "";
        }
        Partial partial = columnMap.get(path);
        if (partial == null) {
            throw new IllegalArgumentException(
                    "Illegal embedded path \"" + path + "\""
            );
        }
        return partial;
    }

    @Override
    public int size() {
        return arr.length;
    }

    @NotNull
    @Override
    public Iterator<String> iterator() {
        return new Itr(arr);
    }

    private static class Itr implements Iterator<String> {

        private final String[] arr;

        private int index;

        private Itr(String[] arr) {
            this.arr = arr;
        }

        @Override
        public boolean hasNext() {
            return index < arr.length;
        }

        @Override
        public String next() {
            if (index < arr.length) {
                return arr[index++];
            }
            throw new NoSuchElementException();
        }
    }

    public static class Partial implements ColumnDefinition {

        private final String path;

        private final String[] arr;

        Partial(String path, List<String> columns) {
            this.path = path;
            this.arr = columns.toArray(EMPTY_ARR);
        }

        public String path() {
            return path;
        }

        @Override
        public int size() {
            return arr.length;
        }

        @NotNull
        @Override
        public Iterator<String> iterator() {
            return new Itr(arr);
        }
    }
}
