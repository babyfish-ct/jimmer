package org.babyfish.jimmer.sql.ast.query.selectable;

import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.query.ConfigurableTypedSubQuery;
import org.babyfish.jimmer.sql.ast.tuple.*;

public interface SubSelectable {

    <R> ConfigurableTypedSubQuery<R> select(
            Selection<R> selection
    );

    <T1, T2> ConfigurableTypedSubQuery<Tuple2<T1, T2>> select(
            Selection<T1> selection1,
            Selection<T2> selection2
    );

    <T1, T2, T3> ConfigurableTypedSubQuery<Tuple3<T1, T2, T3>> select(
            Selection<T1> selection1,
            Selection<T2> selection2,
            Selection<T3> selection3
    );

    <T1, T2, T3, T4> ConfigurableTypedSubQuery<Tuple4<T1, T2, T3, T4>> select(
            Selection<T1> selection1,
            Selection<T2> selection2,
            Selection<T3> selection3,
            Selection<T4> selection4
    );

    <T1, T2, T3, T4, T5> ConfigurableTypedSubQuery<Tuple5<T1, T2, T3, T4, T5>> select(
            Selection<T1> selection1,
            Selection<T2> selection2,
            Selection<T3> selection3,
            Selection<T4> selection4,
            Selection<T5> selection5
    );

    <T1, T2, T3, T4, T5, T6> ConfigurableTypedSubQuery<Tuple6<T1, T2, T3, T4, T5, T6>> select(
            Selection<T1> selection1,
            Selection<T2> selection2,
            Selection<T3> selection3,
            Selection<T4> selection4,
            Selection<T5> selection5,
            Selection<T6> selection6
    );

    <T1, T2, T3, T4, T5, T6, T7> ConfigurableTypedSubQuery<Tuple7<T1, T2, T3, T4, T5, T6, T7>> select(
            Selection<T1> selection1,
            Selection<T2> selection2,
            Selection<T3> selection3,
            Selection<T4> selection4,
            Selection<T5> selection5,
            Selection<T6> selection6,
            Selection<T7> selection7
    );

    <T1, T2, T3, T4, T5, T6, T7, T8> ConfigurableTypedSubQuery<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> select(
            Selection<T1> selection1,
            Selection<T2> selection2,
            Selection<T3> selection3,
            Selection<T4> selection4,
            Selection<T5> selection5,
            Selection<T6> selection6,
            Selection<T7> selection7,
            Selection<T8> selection8
    );

    <T1, T2, T3, T4, T5, T6, T7, T8, T9> ConfigurableTypedSubQuery<Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> select(
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
}
