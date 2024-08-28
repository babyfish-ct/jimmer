package org.babyfish.jimmer.sql.meta;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;

import java.util.*;
import java.util.regex.Pattern;

public class EmbeddedColumns extends MultipleColumns {

    private static final String[] EMPTY_ARR = new String[0];

    private static final Pattern DOT_PATTERN = Pattern.compile("\\.");

    private final Map<String, List<ImmutableProp>> terminatorMap;

    private final List<ImmutableProp>[] propsArr;

    private final Map<String, Partial> partialMap;

    public EmbeddedColumns(Map<String, PathData> pathDataMap, ImmutableType type) {
        super(pathDataMap.get("").columnNames.toArray(EMPTY_ARR), true);
        Map<String, Partial> map = new HashMap<>();
        Map<String, List<ImmutableProp>> terminatorMap = new HashMap<>();
        for (Map.Entry<String, PathData> e : pathDataMap.entrySet()) {
            String key = e.getKey();
            PathData pathData = e.getValue();
            map.put(key, new Partial(key, pathData.columnNames, pathData.isTerminal));
            if (pathData.isTerminal) {
                String[] propNames = DOT_PATTERN.split(key);
                List<ImmutableProp> props = new ArrayList<>(propNames.length);
                ImmutableType t = type;
                for (String propName : propNames) {
                    ImmutableProp prop = t.getProp(propName);
                    props.add(prop);
                    t = prop.getTargetType();
                }
                terminatorMap.put(
                        pathData.columnNames.get(0),
                        Collections.unmodifiableList(props)
                );
            }
        }
        List<ImmutableProp>[] propsArr = new List[size()];
        for (int i = 0; i < propsArr.length; i++) {
            propsArr[i] = terminatorMap.get(name(i));
        }
        this.terminatorMap = terminatorMap;
        this.propsArr = propsArr;
        this.partialMap = map;
    }

    public List<ImmutableProp> path(String columnName) {
        return terminatorMap.get(columnName);
    }

    public List<ImmutableProp> path(int index) {
        return propsArr[index];
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

    @Override
    public boolean isForeignKey() {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        EmbeddedColumns that = (EmbeddedColumns) o;
        return partialMap.equals(that.partialMap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), partialMap);
    }

    @Override
    public String toString() {
        return "EmbeddedColumns{" +
                "partialMap=" + partialMap +
                ", arr=" + Arrays.toString(arr) +
                ", embedded=" + embedded +
                '}';
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

        @Override
        public boolean isForeignKey() {
            return false;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            Partial partial = (Partial) o;
            return path.equals(partial.path);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), path);
        }

        @Override
        public String toString() {
            return "Partial{" +
                    "path='" + path + '\'' +
                    ", arr=" + Arrays.toString(arr) +
                    ", embedded=" + embedded +
                    '}';
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
