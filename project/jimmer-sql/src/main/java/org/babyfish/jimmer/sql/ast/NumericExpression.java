package org.babyfish.jimmer.sql.ast;

import org.babyfish.jimmer.sql.ast.impl.CoalesceBuilder;

import java.math.BigDecimal;

public interface NumericExpression<N extends Number & Comparable<N>> extends ComparableExpression<N> {

    NumericExpression<N> plus(Expression<N> other);

    NumericExpression<N> plus(N other);

    NumericExpression<N> minus(Expression<N> other);

    NumericExpression<N> minus(N other);

    NumericExpression<N> times(Expression<N> other);

    NumericExpression<N> times(N other);

    NumericExpression<N> div(Expression<N> other);

    NumericExpression<N> div(N other);

    NumericExpression<N> rem(Expression<N> other);

    NumericExpression<N> rem(N other);

    Predicate lt(Expression<N> other);

    Predicate lt(N other);

    Predicate le(Expression<N> other);

    Predicate le(N other);

    Predicate gt(Expression<N> other);

    Predicate gt(N other);

    Predicate ge(Expression<N> other);

    Predicate ge(N other);

    Predicate between(Expression<N> min, Expression<N> max);

    Predicate between(N min, N max);

    Predicate notBetween(Expression<N> min, Expression<N> max);

    Predicate notBetween(N min, N max);

    NumericExpression<N> sum();

    NumericExpression<N> min();

    NumericExpression<N> max();

    NumericExpression<BigDecimal> avg();

    @Override
    NumericExpression<N> coalesce(N defaultValue);

    @Override
    NumericExpression<N> coalesce(Expression<N> defaultExpr);

    @Override
    CoalesceBuilder.Num<N> coalesceBuilder();
}