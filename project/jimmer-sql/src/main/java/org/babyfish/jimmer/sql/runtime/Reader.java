package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.sql.ast.tuple.*;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface Reader<T> {

    T read(ResultSet rs, Col col) throws SQLException;

    class Col {

        private int value;

        public int get() {
            return ++value;
        }

        public void add(int delta) {
            value += delta;
        }

        public void reset() {
            value = 0;
        }
    }

    static <T1, T2> Reader<Tuple2<T1, T2>> tuple(Reader<T1> reader1, Reader<T2> reader2) {
        return new Reader<Tuple2<T1, T2>>() {
            @Override
            public Tuple2<T1, T2> read(ResultSet rs, Col col) throws SQLException {
                return new Tuple2<>(reader1.read(rs, col), reader2.read(rs, col));
            }
        };
    }

    static <T1, T2, T3> Reader<Tuple3<T1, T2, T3>> tuple(
            Reader<T1> reader1,
            Reader<T2> reader2,
            Reader<T3> reader3
    ) {
        return new Reader<Tuple3<T1, T2, T3>>() {
            @Override
            public Tuple3<T1, T2, T3> read(ResultSet rs, Col col) throws SQLException {
                return new Tuple3<>(
                        reader1.read(rs, col),
                        reader2.read(rs, col),
                        reader3.read(rs, col)
                );
            }
        };
    }

    static <T1, T2, T3, T4> Reader<Tuple4<T1, T2, T3, T4>> tuple(
            Reader<T1> reader1,
            Reader<T2> reader2,
            Reader<T3> reader3,
            Reader<T4> reader4
    ) {
        return new Reader<Tuple4<T1, T2, T3, T4>>() {
            @Override
            public Tuple4<T1, T2, T3, T4> read(ResultSet rs, Col col) throws SQLException {
                return new Tuple4<>(
                        reader1.read(rs, col),
                        reader2.read(rs, col),
                        reader3.read(rs, col),
                        reader4.read(rs, col)
                );
            }
        };
    }

    static <T1, T2, T3, T4, T5> Reader<Tuple5<T1, T2, T3, T4, T5>> tuple(
            Reader<T1> reader1,
            Reader<T2> reader2,
            Reader<T3> reader3,
            Reader<T4> reader4,
            Reader<T5> reader5
    ) {
        return new Reader<Tuple5<T1, T2, T3, T4, T5>>() {
            @Override
            public Tuple5<T1, T2, T3, T4, T5> read(ResultSet rs, Col col) throws SQLException {
                return new Tuple5<>(
                        reader1.read(rs, col),
                        reader2.read(rs, col),
                        reader3.read(rs, col),
                        reader4.read(rs, col),
                        reader5.read(rs, col)
                );
            }
        };
    }

    static <T1, T2, T3, T4, T5, T6> Reader<Tuple6<T1, T2, T3, T4, T5, T6>> tuple(
            Reader<T1> reader1,
            Reader<T2> reader2,
            Reader<T3> reader3,
            Reader<T4> reader4,
            Reader<T5> reader5,
            Reader<T6> reader6
    ) {
        return new Reader<Tuple6<T1, T2, T3, T4, T5, T6>>() {
            @Override
            public Tuple6<T1, T2, T3, T4, T5, T6> read(ResultSet rs, Col col) throws SQLException {
                return new Tuple6<>(
                        reader1.read(rs, col),
                        reader2.read(rs, col),
                        reader3.read(rs, col),
                        reader4.read(rs, col),
                        reader5.read(rs, col),
                        reader6.read(rs, col)
                );
            }
        };
    }

    static <T1, T2, T3, T4, T5, T6, T7> Reader<Tuple7<T1, T2, T3, T4, T5, T6, T7>> tuple(
            Reader<T1> reader1,
            Reader<T2> reader2,
            Reader<T3> reader3,
            Reader<T4> reader4,
            Reader<T5> reader5,
            Reader<T6> reader6,
            Reader<T7> reader7
    ) {
        return new Reader<Tuple7<T1, T2, T3, T4, T5, T6, T7>>() {
            @Override
            public Tuple7<T1, T2, T3, T4, T5, T6, T7> read(ResultSet rs, Col col) throws SQLException {
                return new Tuple7<>(
                        reader1.read(rs, col),
                        reader2.read(rs, col),
                        reader3.read(rs, col),
                        reader4.read(rs, col),
                        reader5.read(rs, col),
                        reader6.read(rs, col),
                        reader7.read(rs, col)
                );
            }
        };
    }

    static <T1, T2, T3, T4, T5, T6, T7, T8> Reader<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> tuple(
            Reader<T1> reader1,
            Reader<T2> reader2,
            Reader<T3> reader3,
            Reader<T4> reader4,
            Reader<T5> reader5,
            Reader<T6> reader6,
            Reader<T7> reader7,
            Reader<T8> reader8
    ) {
        return new Reader<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>>() {
            @Override
            public Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> read(ResultSet rs, Col col) throws SQLException {
                return new Tuple8<>(
                        reader1.read(rs, col),
                        reader2.read(rs, col),
                        reader3.read(rs, col),
                        reader4.read(rs, col),
                        reader5.read(rs, col),
                        reader6.read(rs, col),
                        reader7.read(rs, col),
                        reader8.read(rs, col)
                );
            }
        };
    }

    static <T1, T2, T3, T4, T5, T6, T7, T8, T9> Reader<Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> tuple(
            Reader<T1> reader1,
            Reader<T2> reader2,
            Reader<T3> reader3,
            Reader<T4> reader4,
            Reader<T5> reader5,
            Reader<T6> reader6,
            Reader<T7> reader7,
            Reader<T8> reader8,
            Reader<T9> reader9
    ) {
        return new Reader<Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>>() {
            @Override
            public Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9> read(ResultSet rs, Col col) throws SQLException {
                return new Tuple9<>(
                        reader1.read(rs, col),
                        reader2.read(rs, col),
                        reader3.read(rs, col),
                        reader4.read(rs, col),
                        reader5.read(rs, col),
                        reader6.read(rs, col),
                        reader7.read(rs, col),
                        reader8.read(rs, col),
                        reader9.read(rs, col)
                );
            }
        };
    }
}
