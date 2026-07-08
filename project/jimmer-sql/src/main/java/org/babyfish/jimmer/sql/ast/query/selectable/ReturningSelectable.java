package org.babyfish.jimmer.sql.ast.query.selectable;

import org.babyfish.jimmer.sql.ast.Executable;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.tuple.*;
import org.babyfish.jimmer.sql.runtime.TupleMapper;

import java.util.List;

public interface ReturningSelectable {

    <R> Executable<List<R>> returning(Selection<R> selection);

    <T1, T2> Executable<List<Tuple2<T1, T2>>> returning(
            Selection<T1> selection1,
            Selection<T2> selection2
    );

    <T1, T2, T3> Executable<List<Tuple3<T1, T2, T3>>> returning(
            Selection<T1> selection1,
            Selection<T2> selection2,
            Selection<T3> selection3
    );

    <T1, T2, T3, T4> Executable<List<Tuple4<T1, T2, T3, T4>>> returning(
            Selection<T1> selection1,
            Selection<T2> selection2,
            Selection<T3> selection3,
            Selection<T4> selection4
    );

    <T1, T2, T3, T4, T5> Executable<List<Tuple5<T1, T2, T3, T4, T5>>> returning(
            Selection<T1> selection1,
            Selection<T2> selection2,
            Selection<T3> selection3,
            Selection<T4> selection4,
            Selection<T5> selection5
    );

    <T1, T2, T3, T4, T5, T6> Executable<List<Tuple6<T1, T2, T3, T4, T5, T6>>> returning(
            Selection<T1> selection1,
            Selection<T2> selection2,
            Selection<T3> selection3,
            Selection<T4> selection4,
            Selection<T5> selection5,
            Selection<T6> selection6
    );

    <T1, T2, T3, T4, T5, T6, T7> Executable<List<Tuple7<T1, T2, T3, T4, T5, T6, T7>>> returning(
            Selection<T1> selection1,
            Selection<T2> selection2,
            Selection<T3> selection3,
            Selection<T4> selection4,
            Selection<T5> selection5,
            Selection<T6> selection6,
            Selection<T7> selection7
    );

    <T1, T2, T3, T4, T5, T6, T7, T8> Executable<List<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>>> returning(
            Selection<T1> selection1,
            Selection<T2> selection2,
            Selection<T3> selection3,
            Selection<T4> selection4,
            Selection<T5> selection5,
            Selection<T6> selection6,
            Selection<T7> selection7,
            Selection<T8> selection8
    );

    <T1, T2, T3, T4, T5, T6, T7, T8, T9> Executable<List<Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>>> returning(
            Selection<T1> selection1,
            Selection<T2> selection2,
            Selection<T3> selection3,
            Selection<T4> selection4,
            Selection<T5> selection5,
            Selection<T6> selection6,
            Selection<T7> selection7,
            Selection<T8> selection8,
            Selection<T9> selection9
    );

    <R> Executable<List<R>> returning(TupleMapper<R> mapper);
}
