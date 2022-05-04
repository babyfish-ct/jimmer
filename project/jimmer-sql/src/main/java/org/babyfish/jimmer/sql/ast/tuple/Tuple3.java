package org.babyfish.jimmer.sql.ast.tuple;

import java.util.Objects;

public class Tuple3<T1, T2, T3> {

    private final T1 _1;

    private final T2 _2;

    private final T3 _3;

    public Tuple3(T1 _1, T2 _2, T3 _3) {
        this._1 = _1;
        this._2 = _2;
        this._3 = _3;
    }

    public T1 _1() {
        return _1;
    }

    public T2 _2() {
        return _2;
    }

    public T3 _3() {
        return _3;
    }

    @Override
    public int hashCode() {
        return Objects.hash(_1, _2, _3);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tuple3<?, ?, ?> other = (Tuple3<?, ?, ?>) o;
        return Objects.equals(_1, other._1) &&
                Objects.equals(_2, other._2) &&
                Objects.equals(_3, other._3);
    }

    @Override
    public String toString() {
        return "Tuple3{" +
                "_1=" + _1 +
                ", _2=" + _2 +
                ", _3=" + _3 +
                '}';
    }
}
