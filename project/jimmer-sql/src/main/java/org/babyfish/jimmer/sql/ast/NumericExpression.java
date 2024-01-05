package org.babyfish.jimmer.sql.ast;

import org.babyfish.jimmer.sql.ast.impl.CoalesceBuilder;
import org.jetbrains.annotations.NotNull;

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

    @NotNull Predicate lt(@NotNull Expression<N> other);

    @NotNull Predicate lt(@NotNull N other);

    @NotNull Predicate le(@NotNull Expression<N> other);

    @NotNull Predicate le(@NotNull N other);

    @NotNull Predicate gt(@NotNull Expression<N> other);

    @NotNull Predicate gt(@NotNull N other);

    @NotNull Predicate ge(@NotNull Expression<N> other);

    @NotNull Predicate ge(@NotNull N other);

    @NotNull Predicate between(@NotNull Expression<N> min, @NotNull Expression<N> max);

    @NotNull Predicate between(@NotNull N min, @NotNull N max);

    @NotNull Predicate notBetween(@NotNull Expression<N> min, @NotNull Expression<N> max);

    @NotNull Predicate notBetween(@NotNull N min, @NotNull N max);

    NumericExpression<N> sum();

    NumericExpression<N> min();

    NumericExpression<N> max();

    NumericExpression<BigDecimal> avg();

    @Override
    @NotNull
    NumericExpression<N> coalesce(N defaultValue);

    @Override
    @NotNull
    NumericExpression<N> coalesce(Expression<N> defaultExpr);

    @Override
    CoalesceBuilder.@NotNull Num<N> coalesceBuilder();
}