package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.NumericExpression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;

interface NumericExpressionImplementor<N extends Number & Comparable<N>> extends NumericExpression<N>, ComparableExpressionImplementor<N> {

    @Override
    default NumericExpression<N> plus(Expression<N> other) {
        return new BinaryExpression.Plus<>(getType(), this, other);
    }

    @Override
    default NumericExpression<N> plus(N other) {
        return plus(Literals.number(other));
    }

    @Override
    default NumericExpression<N> minus(Expression<N> other) {
        return new BinaryExpression.Minus<>(getType(), this, other);
    }

    @Override
    default NumericExpression<N> minus(N other) {
        return minus(Literals.number(other));
    }

    @Override
    default NumericExpression<N> times(Expression<N> other) {
        return new BinaryExpression.Times<>(getType(), this, other);
    }

    @Override
    default NumericExpression<N> times(N other) {
        return times(Literals.number(other));
    }

    @Override
    default NumericExpression<N> div(Expression<N> other) {
        return new BinaryExpression.Div<>(getType(), this, other);
    }

    @Override
    default NumericExpression<N> div(N other) {
        return div(Literals.number(other));
    }

    @Override
    default NumericExpression<N> rem(Expression<N> other) {
        return new BinaryExpression.Rem<>(getType(), this, other);
    }

    @Override
    default NumericExpression<N> rem(N other) {
        return rem(Literals.number(other));
    }

    @Override
    default @NotNull Predicate lt(@NotNull Expression<N> other) {
        return new ComparisonPredicate.Lt(this, other);
    }

    @Override
    default @NotNull Predicate lt(@NotNull N other) {
        return lt(Literals.number(other));
    }

    @Override
    default @NotNull Predicate le(@NotNull Expression<N> other) {
        return new ComparisonPredicate.Le(this, other);
    }

    @Override
    default @NotNull Predicate le(@NotNull N other) {
        return le(Literals.number(other));
    }

    @Override
    default @NotNull Predicate gt(@NotNull Expression<N> other) {
        return new ComparisonPredicate.Gt(this, other);
    }

    @Override
    default @NotNull Predicate gt(@NotNull N other) {
        return gt(Literals.number(other));
    }

    @Override
    default @NotNull Predicate ge(@NotNull Expression<N> other) {
        return new ComparisonPredicate.Ge(this, other);
    }

    @Override
    default @NotNull Predicate ge(@NotNull N other) {
        return ge(Literals.number(other));
    }

    @Override
    default @NotNull Predicate between(@NotNull Expression<N> min, @NotNull Expression<N> max) {
        return new BetweenPredicate(false, this, min, max);
    }

    @Override
    default @NotNull Predicate between(@NotNull N min, @NotNull N max) {
        return new BetweenPredicate(false, this, Literals.any(min), Literals.any(max));
    }

    @Override
    default @NotNull Predicate notBetween(@NotNull Expression<N> min, @NotNull Expression<N> max) {
        return new BetweenPredicate(true, this, min, max);
    }

    @Override
    default @NotNull Predicate notBetween(@NotNull N min, @NotNull N max) {
        return new BetweenPredicate(true, this, Literals.any(min), Literals.any(max));
    }

    @Override
    default NumericExpression<N> sum() {
        return new AggregationExpression.Sum<>(this);
    }

    @Override
    default NumericExpression<N> min() {
        return new AggregationExpression.Min<>(this);
    }

    @Override
    default NumericExpression<N> max() {
        return new AggregationExpression.Max<>(this);
    }

    @Override
    default NumericExpression<Double> avg() {
        return new AggregationExpression.Avg(this);
    }

    @Override
    default NumericExpression<BigDecimal> avgAsDecimal() {
        return new AggregationExpression.AvgAsDecimal(this);
    }

    @Override
    default @NotNull NumericExpression<N> coalesce(N defaultValue) {
        return coalesceBuilder().or(defaultValue).build();
    }

    @Override
    default @NotNull NumericExpression<N> coalesce(Expression<N> defaultExpr) {
        return coalesceBuilder().or(defaultExpr).build();
    }

    @Override
    default CoalesceBuilder.@NotNull Num<N> coalesceBuilder() {
        return new CoalesceBuilder.Num<>(this);
    }
}
