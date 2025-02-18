package org.babyfish.jimmer.sql.ast.query.selectable;

import org.babyfish.jimmer.sql.ast.*;
import org.babyfish.jimmer.sql.ast.impl.query.MergedTypedSubQueryImpl;
import org.babyfish.jimmer.sql.ast.query.ConfigurableSubQuery;
import org.babyfish.jimmer.sql.ast.query.TypedSubQuery;
import org.babyfish.jimmer.sql.ast.tuple.*;

public interface SubSelectable {

    <R> ConfigurableSubQuery<R> select(
            Selection<R> selection
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

    default Str selectString(Selection<String> selection) {
        return (Str) select(selection);
    }

    default <N extends Number & Comparable<N>> Num<N> selectNumber(Selection<N> selection) {
        return (Num<N>) select(selection);
    }

    default <T extends Comparable<T>> Cmp<T> selectComparable(Selection<T> selection) {
        return (Cmp<T>) select(selection);
    }

    default Num<Long> selectCount() {
        return selectNumber(Expression.rowCount());
    }

    interface Str extends ConfigurableSubQuery<String>, StringExpression {

        default Str union(TypedSubQuery<String> other) {
            return (Str) MergedTypedSubQueryImpl.of("union", this, other);
        }

        default Str unionAll(TypedSubQuery<String> other) {
            return (Str)MergedTypedSubQueryImpl.of("union all", this, other);
        }

        default Str minus(TypedSubQuery<String> other) {
            return (Str)MergedTypedSubQueryImpl.of("minus", this, other);
        }

        default Str intersect(TypedSubQuery<String> other) {
            return (Str)MergedTypedSubQueryImpl.of("intersect", this, other);
        }
    }

    interface Num<N extends Number & Comparable<N>> extends ConfigurableSubQuery<N>, NumericExpression<N> {

        default Num<N> union(TypedSubQuery<N> other) {
            return (Num<N>)MergedTypedSubQueryImpl.of("union", this, other);
        }

        default Num<N> unionAll(TypedSubQuery<N> other) {
            return (Num<N>)MergedTypedSubQueryImpl.of("union all", this, other);
        }

        default Num<N> minus(TypedSubQuery<N> other) {
            return (Num<N>)MergedTypedSubQueryImpl.of("minus", this, other);
        }

        default Num<N> intersect(TypedSubQuery<N> other) {
            return (Num<N>)MergedTypedSubQueryImpl.of("intersect", this, other);
        }
    }

    interface Cmp<T extends Comparable<?>> extends ConfigurableSubQuery<T>, ComparableExpression<T> {

        default Cmp<T> union(TypedSubQuery<T> other) {
            return (Cmp<T>)MergedTypedSubQueryImpl.of("union", this, other);
        }

        default Cmp<T> unionAll(TypedSubQuery<T> other) {
            return (Cmp<T>)MergedTypedSubQueryImpl.of("union all", this, other);
        }

        default Cmp<T> minus(TypedSubQuery<T> other) {
            return (Cmp<T>)MergedTypedSubQueryImpl.of("minus", this, other);
        }

        default Cmp<T> intersect(TypedSubQuery<T> other) {
            return (Cmp<T>)MergedTypedSubQueryImpl.of("intersect", this, other);
        }
    }
}
