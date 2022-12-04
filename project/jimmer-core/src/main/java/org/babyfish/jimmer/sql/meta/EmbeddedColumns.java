package org.babyfish.jimmer.sql.meta;

import java.util.*;

public class EmbeddedColumns extends MultipleColumns {

    private static final String[] EMPTY_ARR = new String[0];

    private final Map<String, Partial> columnMap;

    public EmbeddedColumns(Map<String, Path> columnMap) {
        super(columnMap.get("").columnNames.toArray(EMPTY_ARR), true);
        Map<String, Partial> map = new HashMap<>();
        for (Map.Entry<String, Path> e : columnMap.entrySet()) {
            String key = e.getKey();
            Path path = e.getValue();
            map.put(key, new Partial(key, path.columnNames, path.isTerminal));
        }
        this.columnMap = map;
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

    public static class Partial extends MultipleColumns {

        private final String path;

        Partial(String path, List<String> columns, boolean isTerminal) {
            super(columns.toArray(EMPTY_ARR), !isTerminal);
            this.path = path;
        }

        public String path() {
            return path;
        }
    }

    public static class Path {

        public final boolean isTerminal;

        public final List<String> columnNames = new ArrayList<>();

        public Path(boolean isTerminal) {
            this.isTerminal = isTerminal;
        }

        @Override
        public String toString() {
            return (isTerminal ? "column" : "embedded") + columnNames;
        }
    }
}
