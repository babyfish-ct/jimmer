package org.babyfish.jimmer.api;

public class Tuple<T1, T2> {

    private final T1 _1;

    private final T2 _2;

    public Tuple(T1 t1, T2 t2) {
        _1 = t1;
        _2 = t2;
    }

    public T1 get_1() {
        return _1;
    }

    public T2 get_2() {
        return _2;
    }
}
