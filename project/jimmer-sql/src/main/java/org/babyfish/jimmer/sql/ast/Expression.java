package org.babyfish.jimmer.sql.ast;

import org.babyfish.jimmer.sql.ast.impl.Constants;
import org.babyfish.jimmer.sql.ast.impl.Literals;
import org.babyfish.jimmer.sql.ast.query.TypedSubQuery;

import java.util.Arrays;
import java.util.Collection;

public interface Expression<T> extends Selection<T> {

    Predicate eq(Expression<T> other);

    Predicate eq(T other);

    Predicate ne(Expression<T> other);

    Predicate ne(T other);

    Predicate isNull();

    Predicate isNotNull();

    Predicate in(Collection<T> values);

    default Predicate in(T ... values) {
        return in(Arrays.asList(values));
    }

    Predicate notIn(Collection<T> values);

    default Predicate notIn(T ... values) {
        return notIn(Arrays.asList(values));
    }

    Predicate in(TypedSubQuery<T> subQuery);

    Predicate notIn(TypedSubQuery<T> subQuery);

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
}
