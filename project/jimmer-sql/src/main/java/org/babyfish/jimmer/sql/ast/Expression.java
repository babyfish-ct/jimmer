package org.babyfish.jimmer.sql.ast;

import org.babyfish.jimmer.sql.ast.impl.CoalesceBuilder;
import org.babyfish.jimmer.sql.ast.impl.Constants;
import org.babyfish.jimmer.sql.ast.impl.Literals;
import org.babyfish.jimmer.sql.ast.impl.Tuples;
import org.babyfish.jimmer.sql.ast.query.TypedSubQuery;
import org.babyfish.jimmer.sql.ast.tuple.*;

import java.util.Collection;

public interface Expression<T> extends Selection<T> {

    Predicate eq(Expression<T> other);

    Predicate eq(T other);

    Predicate ne(Expression<T> other);

    Predicate ne(T other);

    Predicate isNull();

    Predicate isNotNull();

    Predicate in(Collection<T> values);

    Predicate in(T ... values);

    Predicate notIn(Collection<T> values);

    Predicate notIn(T ... values);

    Predicate in(TypedSubQuery<T> subQuery);

    Predicate notIn(TypedSubQuery<T> subQuery);

    Expression<T> coalesce(T defaultValue);

    CoalesceBuilder<T> coalesceBuilder();

    static <N extends Number> NumericExpression<N> constant(N value) {
        return Constants.number(value);
    }

    static StringExpression string(String value) {
        return Literals.string(value);
    }

    static <N extends Number> NumericExpression<N> number(N value) {
        return Literals.number(value);
    }

    static <T extends Comparable<T>> ComparableExpression<T> comparable(T value) {
        return Literals.comparable(value);
    }

    static <T> Expression<T> any(T value) {
        return Literals.any(value);
    }

    static StringExpression nativeString(
            String sql,
            Expression<?>[] expressions,
            Object[] values
    ) {
        throw new RuntimeException();
    }

    static <N extends Number> NumericExpression<N> nativeNumber(
            Class<N> type,
            String sql,
            Expression<?>[] expressions,
            Object[] values
    ) {
        throw new RuntimeException();
    }

    static <T extends Comparable<T>> ComparableExpression<T> nativeComparable(
            Class<T> type,
            String sql,
            Expression<?>[] expressions,
            Object[] values
    ) {
        throw new RuntimeException();
    }

    static <T> Expression<T> nativeAny(
            Class<T> type,
            String sql,
            Expression<?>[] expressions,
            Object[] values
    ) {
        throw new RuntimeException();
    }

    static <T1, T2> Expression<Tuple2<T1, T2>> tuple(
            Expression<T1> expr1,
            Expression<T2> expr2
    ) {
        return new Tuples.Expr2<>(expr1, expr2);
    }

    static <T1, T2, T3> Expression<Tuple3<T1, T2, T3>> tuple(
            Expression<T1> expr1,
            Expression<T2> expr2,
            Expression<T3> expr3
    ) {
        return new Tuples.Expr3<>(expr1, expr2, expr3);
    }

    static <T1, T2, T3, T4> Expression<Tuple4<T1, T2, T3, T4>> tuple(
            Expression<T1> expr1,
            Expression<T2> expr2,
            Expression<T3> expr3,
            Expression<T4> expr4
    ) {
        return new Tuples.Expr4<>(expr1, expr2, expr3, expr4);
    }

    static <T1, T2, T3, T4, T5> Expression<Tuple5<T1, T2, T3, T4, T5>> tuple(
            Expression<T1> expr1,
            Expression<T2> expr2,
            Expression<T3> expr3,
            Expression<T4> expr4,
            Expression<T5> expr5
    ) {
        return new Tuples.Expr5<>(expr1, expr2, expr3, expr4, expr5);
    }

    static <T1, T2, T3, T4, T5, T6> Expression<Tuple6<T1, T2, T3, T4, T5, T6>> tuple(
            Expression<T1> expr1,
            Expression<T2> expr2,
            Expression<T3> expr3,
            Expression<T4> expr4,
            Expression<T5> expr5,
            Expression<T6> expr6
    ) {
        return new Tuples.Expr6<>(expr1, expr2, expr3, expr4, expr5, expr6);
    }

    static <T1, T2, T3, T4, T5, T6, T7> Expression<Tuple7<T1, T2, T3, T4, T5, T6, T7>> tuple(
            Expression<T1> expr1,
            Expression<T2> expr2,
            Expression<T3> expr3,
            Expression<T4> expr4,
            Expression<T5> expr5,
            Expression<T6> expr6,
            Expression<T7> expr7
    ) {
        return new Tuples.Expr7<>(expr1, expr2, expr3, expr4, expr5, expr6, expr7);
    }

    static <T1, T2, T3, T4, T5, T6, T7, T8> Expression<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> tuple(
            Expression<T1> expr1,
            Expression<T2> expr2,
            Expression<T3> expr3,
            Expression<T4> expr4,
            Expression<T5> expr5,
            Expression<T6> expr6,
            Expression<T7> expr7,
            Expression<T8> expr8
    ) {
        return new Tuples.Expr8<>(expr1, expr2, expr3, expr4, expr5, expr6, expr7, expr8);
    }

    static <T1, T2, T3, T4, T5, T6, T7, T8, T9> Expression<Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> tuple(
        Expression<T1> expr1,
        Expression<T2> expr2,
        Expression<T3> expr3,
        Expression<T4> expr4,
        Expression<T5> expr5,
        Expression<T6> expr6,
        Expression<T7> expr7,
        Expression<T8> expr8,
        Expression<T9> expr9
    ) {
        return new Tuples.Expr9<>(expr1, expr2, expr3, expr4, expr5, expr6, expr7, expr8, expr9);
    }
}
