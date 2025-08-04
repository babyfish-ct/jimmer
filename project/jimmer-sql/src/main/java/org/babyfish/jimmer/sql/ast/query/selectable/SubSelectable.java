package org.babyfish.jimmer.sql.ast.query.selectable;

import org.babyfish.jimmer.sql.ast.*;
import org.babyfish.jimmer.sql.ast.query.ConfigurableSubQuery;
import org.babyfish.jimmer.sql.ast.tuple.*;

import java.time.temporal.Temporal;
import java.util.Date;

public interface SubSelectable {

    <R> ConfigurableSubQuery<R> select(
            Selection<R> selection
    );

    ConfigurableSubQuery.Str select(
            StringExpression selection
    );

    <T extends Comparable<?>> ConfigurableSubQuery.Cmp<T> select(
            ComparableExpression<T> selection
    );

    <N extends Number & Comparable<N>> ConfigurableSubQuery.Num<N> select(
            NumericExpression<N> selection
    );

    <T extends Date> ConfigurableSubQuery.Dt<T> select(
            DateExpression<T> selection
    );

    <T extends Temporal & Comparable<?>> ConfigurableSubQuery.Tp<T> select(
            TemporalExpression<T> selection
    );

    <T1, T2> ConfigurableSubQuery<Tuple2<T1, T2>> select(
            Selection<T1> selection1,
            Selection<T2> selection2
    );

    <T1, T2, T3> ConfigurableSubQuery<Tuple3<T1, T2, T3>> select(
            Selection<T1> selection1,
            Selection<T2> selection2,
            Selection<T3> selection3
    );

    <T1, T2, T3, T4> ConfigurableSubQuery<Tuple4<T1, T2, T3, T4>> select(
            Selection<T1> selection1,
            Selection<T2> selection2,
            Selection<T3> selection3,
            Selection<T4> selection4
    );

    <T1, T2, T3, T4, T5> ConfigurableSubQuery<Tuple5<T1, T2, T3, T4, T5>> select(
            Selection<T1> selection1,
            Selection<T2> selection2,
            Selection<T3> selection3,
            Selection<T4> selection4,
            Selection<T5> selection5
    );

    <T1, T2, T3, T4, T5, T6> ConfigurableSubQuery<Tuple6<T1, T2, T3, T4, T5, T6>> select(
            Selection<T1> selection1,
            Selection<T2> selection2,
            Selection<T3> selection3,
            Selection<T4> selection4,
            Selection<T5> selection5,
            Selection<T6> selection6
    );

    <T1, T2, T3, T4, T5, T6, T7> ConfigurableSubQuery<Tuple7<T1, T2, T3, T4, T5, T6, T7>> select(
            Selection<T1> selection1,
            Selection<T2> selection2,
            Selection<T3> selection3,
            Selection<T4> selection4,
            Selection<T5> selection5,
            Selection<T6> selection6,
            Selection<T7> selection7
    );

    <T1, T2, T3, T4, T5, T6, T7, T8> ConfigurableSubQuery<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> select(
            Selection<T1> selection1,
            Selection<T2> selection2,
            Selection<T3> selection3,
            Selection<T4> selection4,
            Selection<T5> selection5,
            Selection<T6> selection6,
            Selection<T7> selection7,
            Selection<T8> selection8
    );

    <T1, T2, T3, T4, T5, T6, T7, T8, T9> ConfigurableSubQuery<Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> select(
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

    default ConfigurableSubQuery.Str selectString(Selection<String> selection) {
        return (ConfigurableSubQuery.Str) select(selection);
    }

    default <N extends Number & Comparable<N>> ConfigurableSubQuery.Num<N> selectNumber(Selection<N> selection) {
        return (ConfigurableSubQuery.Num<N>) select(selection);
    }

    default <T extends Comparable<T>> ConfigurableSubQuery.Cmp<T> selectComparable(Selection<T> selection) {
        return (ConfigurableSubQuery.Cmp<T>) select(selection);
    }

    default ConfigurableSubQuery.Num<Long> selectCount() {
        return selectNumber(Expression.rowCount());
    }
}
