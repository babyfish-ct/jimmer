package org.babyfish.jimmer.sql.fetcher.impl;

import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.ast.impl.TupleImplementor;
import org.babyfish.jimmer.sql.ast.tuple.*;

import java.util.Map;

class ColumnAccessors {

    private ColumnAccessors() {}

    public static Object get(Object row, int index) {
        if (row == null) {
            return null;
        }
        if (row instanceof ImmutableSpi) {
            if (index != 0) {
                throw new IllegalArgumentException("Illegal index");
            }
            return row;
        }
        if (row instanceof TupleImplementor) {
            if (row instanceof Tuple2<?, ?>) {
                Tuple2<?, ?> tuple = (Tuple2<?, ?>) row;
                switch (index) {
                    case 0:
                        return tuple.get_1();
                    case 1:
                        return tuple.get_2();
                    default:
                        throw new IllegalArgumentException("Illegal index");
                }
            }
            if (row instanceof Tuple3<?, ?, ?>) {
                Tuple3<?, ?, ?> tuple = (Tuple3<?, ?, ?>) row;
                switch (index) {
                    case 0:
                        return tuple.get_1();
                    case 1:
                        return tuple.get_2();
                    case 2:
                        return tuple.get_3();
                    default:
                        throw new IllegalArgumentException("Illegal index");
                }
            }
            if (row instanceof Tuple4<?, ?, ?, ?>) {
                Tuple4<?, ?, ?, ?> tuple = (Tuple4<?, ?, ?, ?>) row;
                switch (index) {
                    case 0:
                        return tuple.get_1();
                    case 1:
                        return tuple.get_2();
                    case 2:
                        return tuple.get_3();
                    case 3:
                        return tuple.get_4();
                    default:
                        throw new IllegalArgumentException("Illegal index");
                }
            }
            if (row instanceof Tuple5<?, ?, ?, ?, ?>) {
                Tuple5<?, ?, ?, ?, ?> tuple = (Tuple5<?, ?, ?, ?, ?>) row;
                switch (index) {
                    case 0:
                        return tuple.get_1();
                    case 1:
                        return tuple.get_2();
                    case 2:
                        return tuple.get_3();
                    case 3:
                        return tuple.get_4();
                    case 4:
                        return tuple.get_5();
                    default:
                        throw new IllegalArgumentException("Illegal index");
                }
            }
            if (row instanceof Tuple6<?, ?, ?, ?, ?, ?>) {
                Tuple6<?, ?, ?, ?, ?, ?> tuple = (Tuple6<?, ?, ?, ?, ?, ?>) row;
                switch (index) {
                    case 0:
                        return tuple.get_1();
                    case 1:
                        return tuple.get_2();
                    case 2:
                        return tuple.get_3();
                    case 3:
                        return tuple.get_4();
                    case 4:
                        return tuple.get_5();
                    case 5:
                        return tuple.get_6();
                    default:
                        throw new IllegalArgumentException("Illegal index");
                }
            }
            if (row instanceof Tuple7<?, ?, ?, ?, ?, ?, ?>) {
                Tuple7<?, ?, ?, ?, ?, ?, ?> tuple = (Tuple7<?, ?, ?, ?, ?, ?, ?>) row;
                switch (index) {
                    case 0:
                        return tuple.get_1();
                    case 1:
                        return tuple.get_2();
                    case 2:
                        return tuple.get_3();
                    case 3:
                        return tuple.get_4();
                    case 4:
                        return tuple.get_5();
                    case 5:
                        return tuple.get_6();
                    case 6:
                        return tuple.get_7();
                    default:
                        throw new IllegalArgumentException("Illegal index");
                }
            }
            if (row instanceof Tuple8<?, ?, ?, ?, ?, ?, ?, ?>) {
                Tuple8<?, ?, ?, ?, ?, ?, ?, ?> tuple = (Tuple8<?, ?, ?, ?, ?, ?, ?, ?>) row;
                switch (index) {
                    case 0:
                        return tuple.get_1();
                    case 1:
                        return tuple.get_2();
                    case 2:
                        return tuple.get_3();
                    case 3:
                        return tuple.get_4();
                    case 4:
                        return tuple.get_5();
                    case 5:
                        return tuple.get_6();
                    case 6:
                        return tuple.get_7();
                    case 7:
                        return tuple.get_8();
                    default:
                        throw new IllegalArgumentException("Illegal index");
                }
            }
            if (row instanceof Tuple9<?, ?, ?, ?, ?, ?, ?, ?, ?>) {
                Tuple9<?, ?, ?, ?, ?, ?, ?, ?, ?> tuple = (Tuple9<?, ?, ?, ?, ?, ?, ?, ?, ?>) row;
                switch (index) {
                    case 0:
                        return tuple.get_1();
                    case 1:
                        return tuple.get_2();
                    case 2:
                        return tuple.get_3();
                    case 3:
                        return tuple.get_4();
                    case 4:
                        return tuple.get_5();
                    case 5:
                        return tuple.get_6();
                    case 6:
                        return tuple.get_7();
                    case 7:
                        return tuple.get_8();
                    case 8:
                        return tuple.get_9();
                    default:
                        throw new IllegalArgumentException("Illegal index");
                }
            }
        }
        throw new AssertionError("Internal bug");
    }

    public static Object set(Object row, Map<Integer, Object> indexValueMap) {
        if (row == null) {
            return null;
        }
        if (row instanceof ImmutableSpi) {
            if (indexValueMap.size() != 1 || !indexValueMap.containsKey(0)) {
                throw new IllegalArgumentException("Illegal indexValueMap");
            }
            return indexValueMap.get(0);
        }
        if (row instanceof TupleImplementor) {
            if (row instanceof Tuple2<?, ?>) {
                Tuple2<?, ?> tuple = (Tuple2<?, ?>) row;
                return new Tuple2<>(
                        indexValueMap.containsKey(0) ? indexValueMap.get(0) : tuple.get_1(),
                        indexValueMap.containsKey(1) ? indexValueMap.get(1) : tuple.get_2()
                );
            }
            if (row instanceof Tuple3<?, ?, ?>) {
                Tuple3<?, ?, ?> tuple = (Tuple3<?, ?, ?>) row;
                return new Tuple3<>(
                        indexValueMap.containsKey(0) ? indexValueMap.get(0) : tuple.get_1(),
                        indexValueMap.containsKey(1) ? indexValueMap.get(1) : tuple.get_2(),
                        indexValueMap.containsKey(2) ? indexValueMap.get(2) : tuple.get_3()
                );
            }
            if (row instanceof Tuple4<?, ?, ?, ?>) {
                Tuple4<?, ?, ?, ?> tuple = (Tuple4<?, ?, ?, ?>) row;
                return new Tuple4<>(
                        indexValueMap.containsKey(0) ? indexValueMap.get(0) : tuple.get_1(),
                        indexValueMap.containsKey(1) ? indexValueMap.get(1) : tuple.get_2(),
                        indexValueMap.containsKey(2) ? indexValueMap.get(2) : tuple.get_3(),
                        indexValueMap.containsKey(3) ? indexValueMap.get(3) : tuple.get_4()
                );
            }
            if (row instanceof Tuple5<?, ?, ?, ?, ?>) {
                Tuple5<?, ?, ?, ?, ?> tuple = (Tuple5<?, ?, ?, ?, ?>) row;
                return new Tuple5<>(
                        indexValueMap.containsKey(0) ? indexValueMap.get(0) : tuple.get_1(),
                        indexValueMap.containsKey(1) ? indexValueMap.get(1) : tuple.get_2(),
                        indexValueMap.containsKey(2) ? indexValueMap.get(2) : tuple.get_3(),
                        indexValueMap.containsKey(3) ? indexValueMap.get(3) : tuple.get_4(),
                        indexValueMap.containsKey(4) ? indexValueMap.get(4) : tuple.get_5()
                );
            }
            if (row instanceof Tuple6<?, ?, ?, ?, ?, ?>) {
                Tuple6<?, ?, ?, ?, ?, ?> tuple = (Tuple6<?, ?, ?, ?, ?, ?>) row;
                return new Tuple6<>(
                        indexValueMap.containsKey(0) ? indexValueMap.get(0) : tuple.get_1(),
                        indexValueMap.containsKey(1) ? indexValueMap.get(1) : tuple.get_2(),
                        indexValueMap.containsKey(2) ? indexValueMap.get(2) : tuple.get_3(),
                        indexValueMap.containsKey(3) ? indexValueMap.get(3) : tuple.get_4(),
                        indexValueMap.containsKey(4) ? indexValueMap.get(4) : tuple.get_5(),
                        indexValueMap.containsKey(5) ? indexValueMap.get(5) : tuple.get_6()
                );
            }
            if (row instanceof Tuple7<?, ?, ?, ?, ?, ?, ?>) {
                Tuple7<?, ?, ?, ?, ?, ?, ?> tuple = (Tuple7<?, ?, ?, ?, ?, ?, ?>) row;
                return new Tuple7<>(
                        indexValueMap.containsKey(0) ? indexValueMap.get(0) : tuple.get_1(),
                        indexValueMap.containsKey(1) ? indexValueMap.get(1) : tuple.get_2(),
                        indexValueMap.containsKey(2) ? indexValueMap.get(2) : tuple.get_3(),
                        indexValueMap.containsKey(3) ? indexValueMap.get(3) : tuple.get_4(),
                        indexValueMap.containsKey(4) ? indexValueMap.get(4) : tuple.get_5(),
                        indexValueMap.containsKey(5) ? indexValueMap.get(5) : tuple.get_6(),
                        indexValueMap.containsKey(6) ? indexValueMap.get(6) : tuple.get_7()
                );
            }
            if (row instanceof Tuple8<?, ?, ?, ?, ?, ?, ?, ?>) {
                Tuple8<?, ?, ?, ?, ?, ?, ?, ?> tuple = (Tuple8<?, ?, ?, ?, ?, ?, ?, ?>) row;
                return new Tuple8<>(
                        indexValueMap.containsKey(0) ? indexValueMap.get(0) : tuple.get_1(),
                        indexValueMap.containsKey(1) ? indexValueMap.get(1) : tuple.get_2(),
                        indexValueMap.containsKey(2) ? indexValueMap.get(2) : tuple.get_3(),
                        indexValueMap.containsKey(3) ? indexValueMap.get(3) : tuple.get_4(),
                        indexValueMap.containsKey(4) ? indexValueMap.get(4) : tuple.get_5(),
                        indexValueMap.containsKey(5) ? indexValueMap.get(5) : tuple.get_6(),
                        indexValueMap.containsKey(6) ? indexValueMap.get(6) : tuple.get_7(),
                        indexValueMap.containsKey(7) ? indexValueMap.get(7) : tuple.get_8()
                );
            }
            if (row instanceof Tuple9<?, ?, ?, ?, ?, ?, ?, ?, ?>) {
                Tuple9<?, ?, ?, ?, ?, ?, ?, ?, ?> tuple = (Tuple9<?, ?, ?, ?, ?, ?, ?, ?, ?>) row;
                return new Tuple9<>(
                        indexValueMap.containsKey(0) ? indexValueMap.get(0) : tuple.get_1(),
                        indexValueMap.containsKey(1) ? indexValueMap.get(1) : tuple.get_2(),
                        indexValueMap.containsKey(2) ? indexValueMap.get(2) : tuple.get_3(),
                        indexValueMap.containsKey(3) ? indexValueMap.get(3) : tuple.get_4(),
                        indexValueMap.containsKey(4) ? indexValueMap.get(4) : tuple.get_5(),
                        indexValueMap.containsKey(5) ? indexValueMap.get(5) : tuple.get_6(),
                        indexValueMap.containsKey(6) ? indexValueMap.get(6) : tuple.get_7(),
                        indexValueMap.containsKey(7) ? indexValueMap.get(7) : tuple.get_8(),
                        indexValueMap.containsKey(8) ? indexValueMap.get(8) : tuple.get_9()
                );
            }
        }
        throw new AssertionError("Internal bug");
    }
}
