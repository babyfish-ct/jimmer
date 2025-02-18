package org.babyfish.jimmer.sql.ast.query;

import org.babyfish.jimmer.sql.ast.*;
import org.babyfish.jimmer.sql.ast.impl.query.MergedTypedSubQueryImpl;

public interface TypedSubQuery<R> extends Expression<R> {

    Expression<R> all();

    Expression<R> any();

    Predicate exists();

    Predicate notExists();

    TypedSubQuery<R> union(TypedSubQuery<R> other);

    TypedSubQuery<R> unionAll(TypedSubQuery<R> other);

    TypedSubQuery<R> minus(TypedSubQuery<R> other);

    TypedSubQuery<R> intersect(TypedSubQuery<R> other);

    interface Str extends TypedSubQuery<String>, StringExpression {

        default Str union(TypedSubQuery<String> other) {
            return (Str)MergedTypedSubQueryImpl.of("union", this, other);
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

    interface Num<N extends Number & Comparable<N>> extends TypedSubQuery<N>, NumericExpression<N> {

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

    interface Cmp<T extends Comparable<?>> extends TypedSubQuery<T>, ComparableExpression<T> {

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
