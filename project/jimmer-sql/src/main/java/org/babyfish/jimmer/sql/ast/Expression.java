package org.babyfish.jimmer.sql.ast;

import org.babyfish.jimmer.sql.ast.query.TypedSubQuery;

import java.util.Collection;

public interface Expression<T> extends Selection<T> {

    Predicate eq(Expression<T> other);

    Predicate ne(Expression<T> other);

    Predicate isNull();

    Predicate isNotNull();

    Predicate in(Collection<T> values);

    Predicate notIn(Collection<T> values);

    Predicate in(TypedSubQuery<T> subQuery);

    Predicate notIn(TypedSubQuery<T> subQuery);

    static StringExpression stringSqlExpression(
            String sql,
            Expression<?>[] expressions,
            Object[] values
    ) {
        throw new RuntimeException();
    }

    static <N extends Number> NumericExpression<N> numericSqlExpression(
            Class<N> type,
            String sql,
            Expression<?>[] expressions,
            Object[] values
    ) {
        throw new RuntimeException();
    }

    static <T extends Comparable<T>> ComparableExpression<T> comparableSqlExpression(
            Class<T> type,
            String sql,
            Expression<?>[] expressions,
            Object[] values
    ) {
        throw new RuntimeException();
    }

    static <T> Expression<T> sqlExpression(
            Class<T> type,
            String sql,
            Expression<?>[] expressions,
            Object[] values
    ) {
        throw new RuntimeException();
    }
}
