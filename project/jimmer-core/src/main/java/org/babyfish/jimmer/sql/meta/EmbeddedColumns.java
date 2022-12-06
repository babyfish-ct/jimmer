package org.babyfish.jimmer.sql.meta;

import java.util.*;

public class EmbeddedColumns extends MultipleColumns {

    private static final String[] EMPTY_ARR = new String[0];

    private final Map<String, Partial> partialMap;

    public EmbeddedColumns(Map<String, PathData> pathDataMap) {
        super(pathDataMap.get("").columnNames.toArray(EMPTY_ARR), true);
        Map<String, Partial> map = new HashMap<>();
        for (Map.Entry<String, PathData> e : pathDataMap.entrySet()) {
            String key = e.getKey();
            PathData pathData = e.getValue();
            map.put(key, new Partial(key, pathData.columnNames, pathData.isTerminal));
        }
        this.partialMap = map;
    }

    public Partial partial(String path) {
        if (path == null) {
            path = "";
        }
        Partial partial = partialMap.get(path);
        if (partial == null) {
            throw new IllegalArgumentException(
                    "Illegal embedded path \"" + path + "\""
            );
        }
        return partial;
    }

    public Map<String, Partial> getPartialMap() {
        return Collections.unmodifiableMap(partialMap);
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

    public static class PathData {

        public final boolean isTerminal;

        public final List<String> columnNames = new ArrayList<>();

        public PathData(boolean isTerminal) {
            this.isTerminal = isTerminal;
        }

        @Override
        public String toString() {
            return (isTerminal ? "column" : "embedded") + columnNames;
        }
    }
}
