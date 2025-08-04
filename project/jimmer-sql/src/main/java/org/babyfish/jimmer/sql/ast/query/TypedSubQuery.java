package org.babyfish.jimmer.sql.ast.query;

import org.babyfish.jimmer.sql.ast.*;
import org.babyfish.jimmer.sql.ast.impl.*;
import org.babyfish.jimmer.sql.ast.impl.query.MergedTypedSubQueryImpl;

import java.time.temporal.Temporal;
import java.util.Date;

public interface TypedSubQuery<R> extends ExpressionImplementor<R> {

    Expression<R> all();

    Expression<R> any();

    Predicate exists();

    Predicate notExists();

    @SafeVarargs
    static <R> TypedSubQuery<R> union(TypedSubQuery<R> ... queries) {
        return MergedTypedSubQueryImpl.of("union", queries);
    }

    @SafeVarargs
    static <R> TypedSubQuery<R> unionAll(TypedSubQuery<R> ... queries) {
        return MergedTypedSubQueryImpl.of("union all", queries);
    }

    @SafeVarargs
    static <R> TypedSubQuery<R> minus(TypedSubQuery<R> ... queries) {
        return MergedTypedSubQueryImpl.of("minus", queries);
    }

    @SafeVarargs
    static <R> TypedSubQuery<R> intersect(TypedSubQuery<R> ... queries) {
        return MergedTypedSubQueryImpl.of("intersect", queries);
    }

    /**
     * @deprecated Please use the static method {@link #union(TypedSubQuery[])}
     */
    @Deprecated
    default TypedSubQuery<R> union(TypedSubQuery<R> other) {
        return TypedSubQuery.union(this, other);
    }

    /**
     * @deprecated Please use the static method {@link #unionAll(TypedSubQuery[])}
     */
    @Deprecated
    default TypedSubQuery<R> unionAll(TypedSubQuery<R> other) {
        return TypedSubQuery.unionAll(this, other);
    }

    /**
     * @deprecated Please use the static method {@link #minus(TypedSubQuery[])}
     */
    @Deprecated
    default TypedSubQuery<R> minus(TypedSubQuery<R> other) {
        return TypedSubQuery.minus(this, other);
    }

    /**
     * @deprecated Please use the static method {@link #intersect(TypedSubQuery[])}
     */
    @Deprecated
    default TypedSubQuery<R> intersect(TypedSubQuery<R> other) {
        return TypedSubQuery.intersect(this, other);
    }

    interface Str extends TypedSubQuery<String>, StringExpressionImplementor {

        @SafeVarargs
        static Str union(TypedSubQuery<String> ... queries) {
            return (Str)MergedTypedSubQueryImpl.of("union", queries);
        }

        @SafeVarargs
        static Str unionAll(TypedSubQuery<String> ... queries) {
            return (Str)MergedTypedSubQueryImpl.of("union all", queries);
        }

        @SafeVarargs
        static Str minus(TypedSubQuery<String> ... queries) {
            return (Str)MergedTypedSubQueryImpl.of("minus", queries);
        }

        @SafeVarargs
        static Str intersect(TypedSubQuery<String> ... queries) {
            return (Str)MergedTypedSubQueryImpl.of("intersect", queries);
        }

        /**
         * @deprecated Please use the static method {@link #intersect(TypedSubQuery[])}
         */
        @Deprecated
        default Str union(TypedSubQuery<String> other) {
            return (Str)MergedTypedSubQueryImpl.of("union", this, other);
        }

        /**
         * @deprecated Please use the static method {@link #intersect(TypedSubQuery[])}
         */
        @Deprecated
        default Str unionAll(TypedSubQuery<String> other) {
            return (Str)MergedTypedSubQueryImpl.of("union all", this, other);
        }

        /**
         * @deprecated Please use the static method {@link #intersect(TypedSubQuery[])}
         */
        @Deprecated
        default Str minus(TypedSubQuery<String> other) {
            return (Str)MergedTypedSubQueryImpl.of("minus", this, other);
        }

        /**
         * @deprecated Please use the static method {@link #intersect(TypedSubQuery[])}
         */
        @Deprecated
        default Str intersect(TypedSubQuery<String> other) {
            return (Str)MergedTypedSubQueryImpl.of("intersect", this, other);
        }
    }

    interface Num<N extends Number & Comparable<N>> extends TypedSubQuery<N>, NumericExpressionImplementor<N> {

        @SafeVarargs
        static <N extends Number & Comparable<N>> Num<N> union(TypedSubQuery<N> ... queries) {
            return (Num<N>)MergedTypedSubQueryImpl.of("union", queries);
        }

        @SafeVarargs
        static <N extends Number & Comparable<N>> Num<N> unionAll(TypedSubQuery<N> ... queries) {
            return (Num<N>)MergedTypedSubQueryImpl.of("union all", queries);
        }

        @SafeVarargs
        static <N extends Number & Comparable<N>> Num<N> minus(TypedSubQuery<N> ... queries) {
            return (Num<N>)MergedTypedSubQueryImpl.of("minus", queries);
        }

        @SafeVarargs
        static <N extends Number & Comparable<N>> Num<N> intersect(TypedSubQuery<N> ... queries) {
            return (Num<N>)MergedTypedSubQueryImpl.of("intersect", queries);
        }

        /**
         * @deprecated Please use the static method {@link #intersect(TypedSubQuery[])}
         */
        @Deprecated
        default Num<N> union(TypedSubQuery<N> other) {
            return (Num<N>)MergedTypedSubQueryImpl.of("union", this, other);
        }

        /**
         * @deprecated Please use the static method {@link #intersect(TypedSubQuery[])}
         */
        @Deprecated
        default Num<N> unionAll(TypedSubQuery<N> other) {
            return (Num<N>)MergedTypedSubQueryImpl.of("union all", this, other);
        }

        /**
         * @deprecated Please use the static method {@link #intersect(TypedSubQuery[])}
         */
        @Deprecated
        default Num<N> minus(TypedSubQuery<N> other) {
            return (Num<N>)MergedTypedSubQueryImpl.of("minus", this, other);
        }

        /**
         * @deprecated Please use the static method {@link #intersect(TypedSubQuery[])}
         */
        @Deprecated
        default Num<N> intersect(TypedSubQuery<N> other) {
            return (Num<N>)MergedTypedSubQueryImpl.of("intersect", this, other);
        }
    }

    interface Cmp<T extends Comparable<?>> extends TypedSubQuery<T>, ComparableExpressionImplementor<T> {

        @SafeVarargs
        static <T extends Comparable<?>> Cmp<T> union(TypedSubQuery<T> ... queries) {
            return (Cmp<T>)MergedTypedSubQueryImpl.of("union", queries);
        }

        @SafeVarargs
        static <T extends Comparable<?>> Cmp<T> unionAll(TypedSubQuery<T> ... queries) {
            return (Cmp<T>)MergedTypedSubQueryImpl.of("union all", queries);
        }

        @SafeVarargs
        static <T extends Comparable<?>> Cmp<T> minus(TypedSubQuery<T> ... queries) {
            return (Cmp<T>)MergedTypedSubQueryImpl.of("minus", queries);
        }

        @SafeVarargs
        static <T extends Comparable<?>> Cmp<T> intersect(TypedSubQuery<T> ... queries) {
            return (Cmp<T>)MergedTypedSubQueryImpl.of("intersect", queries);
        }

        /**
         * @deprecated Please use the static method {@link #intersect(TypedSubQuery[])}
         */
        @Deprecated
        default Cmp<T> union(TypedSubQuery<T> other) {
            return (Cmp<T>)MergedTypedSubQueryImpl.of("union", this, other);
        }

        /**
         * @deprecated Please use the static method {@link #intersect(TypedSubQuery[])}
         */
        @Deprecated
        default Cmp<T> unionAll(TypedSubQuery<T> other) {
            return (Cmp<T>)MergedTypedSubQueryImpl.of("union all", this, other);
        }

        /**
         * @deprecated Please use the static method {@link #intersect(TypedSubQuery[])}
         */
        @Deprecated
        default Cmp<T> minus(TypedSubQuery<T> other) {
            return (Cmp<T>)MergedTypedSubQueryImpl.of("minus", this, other);
        }

        /**
         * @deprecated Please use the static method {@link #intersect(TypedSubQuery[])}
         */
        @Deprecated
        default Cmp<T> intersect(TypedSubQuery<T> other) {
            return (Cmp<T>)MergedTypedSubQueryImpl.of("intersect", this, other);
        }
    }

    interface Dt<T extends Date> extends TypedSubQuery<T>, DateExpressionImplementor<T> {

        @SafeVarargs
        static <T extends Date> Dt<T> union(TypedSubQuery<T> ... queries) {
            return (Dt<T>)MergedTypedSubQueryImpl.of("union", queries);
        }

        @SafeVarargs
        static <T extends Date> Dt<T> unionAll(TypedSubQuery<T> ... queries) {
            return (Dt<T>)MergedTypedSubQueryImpl.of("union all", queries);
        }

        @SafeVarargs
        static <T extends Date> Dt<T> minus(TypedSubQuery<T> ... queries) {
            return (Dt<T>)MergedTypedSubQueryImpl.of("minus", queries);
        }

        @SafeVarargs
        static <T extends Date> Dt<T> intersect(TypedSubQuery<T> ... queries) {
            return (Dt<T>)MergedTypedSubQueryImpl.of("intersect", queries);
        }

        /**
         * @deprecated Please use the static method {@link #intersect(TypedSubQuery[])}
         */
        @Deprecated
        default Dt<T> union(TypedSubQuery<T> other) {
            return (Dt<T>)MergedTypedSubQueryImpl.of("union", this, other);
        }

        /**
         * @deprecated Please use the static method {@link #intersect(TypedSubQuery[])}
         */
        @Deprecated
        default Dt<T> unionAll(TypedSubQuery<T> other) {
            return (Dt<T>)MergedTypedSubQueryImpl.of("union all", this, other);
        }

        /**
         * @deprecated Please use the static method {@link #intersect(TypedSubQuery[])}
         */
        @Deprecated
        default Dt<T> minus(TypedSubQuery<T> other) {
            return (Dt<T>)MergedTypedSubQueryImpl.of("minus", this, other);
        }

        /**
         * @deprecated Please use the static method {@link #intersect(TypedSubQuery[])}
         */
        @Deprecated
        default Dt<T> intersect(TypedSubQuery<T> other) {
            return (Dt<T>)MergedTypedSubQueryImpl.of("intersect", this, other);
        }
    }

    interface Tp<T extends Temporal & Comparable<?>> extends TypedSubQuery<T>, TemporalExpressionImplementor<T> {

        @SafeVarargs
        static <T extends Temporal & Comparable<?>> Tp<T> union(TypedSubQuery<T> ... queries) {
            return (Tp<T>)MergedTypedSubQueryImpl.of("union", queries);
        }

        @SafeVarargs
        static <T extends Temporal & Comparable<?>> Tp<T> unionAll(TypedSubQuery<T> ... queries) {
            return (Tp<T>)MergedTypedSubQueryImpl.of("union all", queries);
        }

        @SafeVarargs
        static <T extends Temporal & Comparable<?>> Tp<T> minus(TypedSubQuery<T> ... queries) {
            return (Tp<T>)MergedTypedSubQueryImpl.of("minus", queries);
        }

        @SafeVarargs
        static <T extends Temporal & Comparable<?>> Tp<T> intersect(TypedSubQuery<T> ... queries) {
            return (Tp<T>)MergedTypedSubQueryImpl.of("intersect", queries);
        }

        /**
         * @deprecated Please use the static method {@link #intersect(TypedSubQuery[])}
         */
        @Deprecated
        default Tp<T> union(TypedSubQuery<T> other) {
            return (Tp<T>)MergedTypedSubQueryImpl.of("union", this, other);
        }

        /**
         * @deprecated Please use the static method {@link #intersect(TypedSubQuery[])}
         */
        @Deprecated
        default Tp<T> unionAll(TypedSubQuery<T> other) {
            return (Tp<T>)MergedTypedSubQueryImpl.of("union all", this, other);
        }

        /**
         * @deprecated Please use the static method {@link #intersect(TypedSubQuery[])}
         */
        @Deprecated
        default Tp<T> minus(TypedSubQuery<T> other) {
            return (Tp<T>)MergedTypedSubQueryImpl.of("minus", this, other);
        }

        /**
         * @deprecated Please use the static method {@link #intersect(TypedSubQuery[])}
         */
        @Deprecated
        default Tp<T> intersect(TypedSubQuery<T> other) {
            return (Tp<T>)MergedTypedSubQueryImpl.of("intersect", this, other);
        }
    }
}
