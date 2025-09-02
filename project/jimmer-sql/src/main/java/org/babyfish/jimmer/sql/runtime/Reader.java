package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.runtime.DraftContext;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.sql.ast.tuple.*;
import org.babyfish.jimmer.sql.dialect.Dialect;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZoneId;

public interface Reader<T> {

    T read(ResultSet rs, Context ctx) throws SQLException;

    void skip(Context ctx);

    final class Context {

        private DraftContext draftContext;

        private final Dialect dialect;

        private final ZoneId zoneId;

        private int col;

        public Context(@Nullable DraftContext draftContext, JSqlClientImplementor sqlClient) {
            this.draftContext = draftContext;
            this.dialect = sqlClient.getDialect();
            this.zoneId = sqlClient.getZoneId();
        }

        @NotNull
        public DraftContext draftContext() {
            DraftContext dc = draftContext;
            if (dc == null) {
                draftContext = dc = new DraftContext(null);
            }
            return dc;
        }

        public Dialect getDialect() {
            return dialect;
        }

        public ZoneId getZoneId() {
            return zoneId;
        }

        public int col() {
            return ++col;
        }

        public void addCol(int delta) {
            col += delta;
        }

        public void resetCol() {
            col = 0;
        }

        public Object resolve(DraftSpi spi) {
            return draftContext().resolveObject(spi);
        }
    }

    static <T1, T2> Reader<Tuple2<T1, T2>> tuple(Reader<T1> reader1, Reader<T2> reader2) {
        return new Reader<Tuple2<T1, T2>>() {
            @Override
            public void skip(Context ctx) {
                reader1.skip(ctx);
                reader2.skip(ctx);
            }

            @Override
            public Tuple2<T1, T2> read(ResultSet rs, Context ctx) throws SQLException {
                return new Tuple2<>(
                        reader1.read(rs, ctx),
                        reader2.read(rs, ctx)
                );
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
            public void skip(Context ctx) {
                reader1.skip(ctx);
                reader2.skip(ctx);
                reader3.skip(ctx);
            }

            @Override
            public Tuple3<T1, T2, T3> read(ResultSet rs, Context ctx) throws SQLException {
                return new Tuple3<>(
                        reader1.read(rs, ctx),
                        reader2.read(rs, ctx),
                        reader3.read(rs, ctx)
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
            public void skip(Context ctx) {
                reader1.skip(ctx);
                reader2.skip(ctx);
                reader3.skip(ctx);
                reader4.skip(ctx);
            }

            @Override
            public Tuple4<T1, T2, T3, T4> read(ResultSet rs, Context ctx) throws SQLException {
                return new Tuple4<>(
                        reader1.read(rs, ctx),
                        reader2.read(rs, ctx),
                        reader3.read(rs, ctx),
                        reader4.read(rs, ctx)
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
            public void skip(Context ctx) {
                reader1.skip(ctx);
                reader2.skip(ctx);
                reader3.skip(ctx);
                reader4.skip(ctx);
                reader5.skip(ctx);
            }

            @Override
            public Tuple5<T1, T2, T3, T4, T5> read(ResultSet rs, Context ctx) throws SQLException {
                return new Tuple5<>(
                        reader1.read(rs, ctx),
                        reader2.read(rs, ctx),
                        reader3.read(rs, ctx),
                        reader4.read(rs, ctx),
                        reader5.read(rs, ctx)
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
            public void skip(Context ctx) {
                reader1.skip(ctx);
                reader2.skip(ctx);
                reader3.skip(ctx);
                reader4.skip(ctx);
                reader5.skip(ctx);
                reader6.skip(ctx);
            }

            @Override
            public Tuple6<T1, T2, T3, T4, T5, T6> read(ResultSet rs, Context ctx) throws SQLException {
                return new Tuple6<>(
                        reader1.read(rs, ctx),
                        reader2.read(rs, ctx),
                        reader3.read(rs, ctx),
                        reader4.read(rs, ctx),
                        reader5.read(rs, ctx),
                        reader6.read(rs, ctx)
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
            public void skip(Context ctx) {
                reader1.skip(ctx);
                reader2.skip(ctx);
                reader3.skip(ctx);
                reader4.skip(ctx);
                reader5.skip(ctx);
                reader6.skip(ctx);
                reader7.skip(ctx);
            }

            @Override
            public Tuple7<T1, T2, T3, T4, T5, T6, T7> read(ResultSet rs, Context ctx) throws SQLException {
                return new Tuple7<>(
                        reader1.read(rs, ctx),
                        reader2.read(rs, ctx),
                        reader3.read(rs, ctx),
                        reader4.read(rs, ctx),
                        reader5.read(rs, ctx),
                        reader6.read(rs, ctx),
                        reader7.read(rs, ctx)
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
            public void skip(Context ctx) {
                reader1.skip(ctx);
                reader2.skip(ctx);
                reader3.skip(ctx);
                reader4.skip(ctx);
                reader5.skip(ctx);
                reader6.skip(ctx);
                reader7.skip(ctx);
                reader8.skip(ctx);
            }

            @Override
            public Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> read(ResultSet rs, Context ctx) throws SQLException {
                return new Tuple8<>(
                        reader1.read(rs, ctx),
                        reader2.read(rs, ctx),
                        reader3.read(rs, ctx),
                        reader4.read(rs, ctx),
                        reader5.read(rs, ctx),
                        reader6.read(rs, ctx),
                        reader7.read(rs, ctx),
                        reader8.read(rs, ctx)
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
            public void skip(Context ctx) {
                reader1.skip(ctx);
                reader2.skip(ctx);
                reader3.skip(ctx);
                reader4.skip(ctx);
                reader5.skip(ctx);
                reader6.skip(ctx);
                reader7.skip(ctx);
                reader8.skip(ctx);
                reader9.skip(ctx);
            }

            @Override
            public Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9> read(ResultSet rs, Context ctx) throws SQLException {
                return new Tuple9<>(
                        reader1.read(rs, ctx),
                        reader2.read(rs, ctx),
                        reader3.read(rs, ctx),
                        reader4.read(rs, ctx),
                        reader5.read(rs, ctx),
                        reader6.read(rs, ctx),
                        reader7.read(rs, ctx),
                        reader8.read(rs, ctx),
                        reader9.read(rs, ctx)
                );
            }
        };
    }

    static Reader<?> tuple(Reader<?>[] readers, TupleCreator<?> tupleCreator) {
        return new Reader<Object>() {
            @Override
            public void skip(Context ctx) {
                for (Reader<?> reader : readers) {
                    reader.skip(ctx);
                }
            }

            @Override
            public Object read(ResultSet rs, Context ctx) throws SQLException {
                int size = readers.length;
                Object[] args = new Object[size];
                for (int i = 0; i < size; i++) {
                    args[i] = readers[i].read(rs, ctx);
                }
                if (tupleCreator != null) {
                    return tupleCreator.createTuple(args);
                }
                return args;
            }
        };
    }
}
