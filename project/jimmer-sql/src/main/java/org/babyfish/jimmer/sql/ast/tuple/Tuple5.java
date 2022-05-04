package org.babyfish.jimmer.sql.ast.tuple;

import java.util.Objects;

public class Tuple5<T1, T2, T3, T4, T5> {

    private final T1 _1;

    private final T2 _2;

    private final T3 _3;

    private final T4 _4;

    private final T5 _5;

    public Tuple5(T1 _1, T2 _2, T3 _3, T4 _4, T5 _5) {
        this._1 = _1;
        this._2 = _2;
        this._3 = _3;
        this._4 = _4;
        this._5 = _5;
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

    public T4 _4() {
        return _4;
    }

    public T5 _5() {
        return _5;
    }

    @Override
    public int hashCode() {
        return Objects.hash(_1, _2, _3, _4, _5);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tuple5<?, ?, ?, ?, ?> other = (Tuple5<?, ?, ?, ?, ?>) o;
        return Objects.equals(_1, other._1) &&
                Objects.equals(_2, other._2) &&
                Objects.equals(_3, other._3) &&
                Objects.equals(_4, other._4) &&
                Objects.equals(_5, other._5);
    }

    @Override
    public String toString() {
        return "Tuple5{" +
                "_1=" + _1 +
                ", _2=" + _2 +
                ", _3=" + _3 +
                ", _4=" + _4 +
                ", _5=" + _5 +
                '}';
    }
}
