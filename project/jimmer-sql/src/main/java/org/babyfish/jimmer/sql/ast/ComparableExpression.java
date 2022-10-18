package org.babyfish.jimmer.sql.ast;

import org.babyfish.jimmer.sql.ast.impl.CoalesceBuilder;

public interface ComparableExpression<T extends Comparable<?>> extends Expression<T> {

    Predicate lt(Expression<T> other);

    Predicate lt(T other);

    Predicate le(Expression<T> other);

    Predicate le(T other);

    Predicate gt(Expression<T> other);

    Predicate gt(T other);

    Predicate ge(Expression<T> other);

    Predicate ge(T other);

    Predicate between(Expression<T> min, Expression<T> max);

    Predicate between(T min, T max);

    Predicate notBetween(Expression<T> min, Expression<T> max);

    Predicate notBetween(T min, T max);

    @Override
    ComparableExpression<T> coalesce(T defaultValue);

    @Override
    ComparableExpression<T> coalesce(Expression<T> defaultExpr);

    @Override
    CoalesceBuilder.Cmp<T> coalesceBuilder();
}
