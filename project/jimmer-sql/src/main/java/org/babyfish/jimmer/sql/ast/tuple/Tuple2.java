package org.babyfish.jimmer.sql.ast.tuple;

import java.util.*;

public class Tuple2<T1, T2> {

    private final T1 _1;

    private final T2 _2;

    public Tuple2(T1 _1, T2 _2) {
        this._1 = _1;
        this._2 = _2;
    }

    public T1 _1() {
        return _1;
    }

    public T2 _2() {
        return _2;
    }

    @Override
    public int hashCode() {
        return Objects.hash(_1, _2);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tuple2<?, ?> other = (Tuple2<?, ?>) o;
        return Objects.equals(_1, other._1) &&
                Objects.equals(_2, other._2);
    }

    @Override
    public String toString() {
        return "Tuple2{" +
                "_1=" + _1 +
                ", _2=" + _2 +
                '}';
    }

    public static <K, V> Map<K, V> toMap(Collection<Tuple2<K, V>> tuples) {
        Map<K, V> map = new LinkedHashMap<>((tuples.size() * 4 + 2) / 3);
        for (Tuple2<K, V> tuple : tuples) {
            map.put(tuple._1, tuple._2);
        }
        return map;
    }

    public static <K, V> Collection<Tuple2<K, V>> fromMap(Map<K, V> map) {
        List<Tuple2<K, V>> list = new ArrayList<>(map.size());
        for (Map.Entry<K, V> e : map.entrySet()) {
            list.add(new Tuple2<>(e.getKey(), e.getValue()));
        }
        return list;
    }
}
