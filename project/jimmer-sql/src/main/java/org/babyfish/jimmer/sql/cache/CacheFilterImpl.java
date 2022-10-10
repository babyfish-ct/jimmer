package org.babyfish.jimmer.sql.cache;

import org.babyfish.jimmer.sql.ast.tuple.Tuple2;

import java.util.*;

class CacheFilterImpl implements CacheFilter {

    private final NavigableMap<String, Object> args;

    CacheFilterImpl(Map<String, Object> args) {
        if (args == null || args.isEmpty()) {
            this.args = Collections.emptyNavigableMap();
        } else {
            this.args = new TreeMap<>(args);
        }
    }

    public CacheFilterImpl(List<Tuple2<String, Object>> tuples) {
        NavigableMap<String, Object> map = new TreeMap<>();
        for (Tuple2<String, Object> tuple : tuples) {
            map.put(tuple.get_1(), tuple.get_2());
        }
        this.args = map;
    }

    @Override
    public NavigableMap<String, Object> getArgs() {
        return args;
    }

    @Override
    public int hashCode() {
        return Objects.hash(args);
    }

    @Override
    public String toString() {
        return "CacheFilterImpl{" +
                "args=" + args +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CacheFilterImpl that = (CacheFilterImpl) o;
        return args.equals(that.args);
    }
}
