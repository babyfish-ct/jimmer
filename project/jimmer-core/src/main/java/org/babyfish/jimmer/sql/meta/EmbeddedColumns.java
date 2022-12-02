package org.babyfish.jimmer.sql.meta;

import java.util.*;

public class EmbeddedColumns extends MultipleColumns {

    private static final String[] EMPTY_ARR = new String[0];

    private final Map<String, Partial> columnMap;

    public EmbeddedColumns(Map<String, List<String>> columnMap) {
        super(columnMap.get("").toArray(EMPTY_ARR));
        Map<String, Partial> map = new HashMap<>();
        for (Map.Entry<String, List<String>> e : columnMap.entrySet()) {
            String key = e.getKey();
            List<String> value = e.getValue();
            map.put(key, new Partial(key, value));
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

        Partial(String path, List<String> columns) {
            super(columns.toArray(EMPTY_ARR));
            this.path = path;
        }

        public String path() {
            return path;
        }
    }
}
