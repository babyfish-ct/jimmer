package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.ComparableExpression;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.jetbrains.annotations.NotNull;

interface ComparableExpressionImplementor<T extends Comparable<?>> extends ComparableExpression<T>, ExpressionImplementor<T> {

    @Override
    default @NotNull Predicate lt(@NotNull Expression<T> other) {
        return new ComparisonPredicate.Lt(
                this,
                ParameterUtils.validate("lt", "other", other)
        );
    }

    @Override
    default @NotNull Predicate lt(@NotNull T other) {
        return new ComparisonPredicate.Lt(
                this,
                Expression.comparable().value(
                        ParameterUtils.validate("lt", "other", other)
                )
        );
    }

    @Override
    default @NotNull Predicate le(@NotNull Expression<T> other) {
        return new ComparisonPredicate.Le(
                this,
                ParameterUtils.validate("le", "other", other)
        );
    }

    @Override
    default @NotNull Predicate le(@NotNull T other) {
        return new ComparisonPredicate.Le(
                this,
                Expression.comparable().value(
                        ParameterUtils.validate("le", "other", other)
                )
        );
    }

    @Override
    default @NotNull Predicate gt(@NotNull Expression<T> other) {
        return new ComparisonPredicate.Gt(
                this,
                ParameterUtils.validate("gt", "other", other)
        );
    }

    @Override
    default @NotNull Predicate gt(@NotNull T other) {
        return new ComparisonPredicate.Gt(
                this,
                Expression.comparable().value(
                        ParameterUtils.validate("gt", "other", other)
                )
        );
    }

    @Override
    default @NotNull Predicate ge(@NotNull Expression<T> other) {
        return new ComparisonPredicate.Ge(
                this,
                ParameterUtils.validate("ge", "other", other)
        );
    }

    @Override
    default @NotNull Predicate ge(@NotNull T other) {
        return new ComparisonPredicate.Ge(
                this,
                Expression.comparable().value(
                        ParameterUtils.validate("ge", "other", other)
                )
        );
    }

    @Override
    default @NotNull Predicate between(@NotNull Expression<T> min, @NotNull Expression<T> max) {
        return new BetweenPredicate(
                false,
                this,
                ParameterUtils.validate("between", "min", min),
                ParameterUtils.validate("between", "max", max)
        );
    }

    @Override
    default @NotNull Predicate between(@NotNull T min, @NotNull T max) {
        return new BetweenPredicate(
                false,
                this,
                Literals.any(
                        ParameterUtils.validate("between", "min", min)
                ),
                Literals.any(
                        ParameterUtils.validate("between", "max", max)
                )
        );
    }

    @Override
    default @NotNull Predicate notBetween(@NotNull Expression<T> min, @NotNull Expression<T> max) {
        return new BetweenPredicate(
                true,
                this,
                ParameterUtils.validate("notBetween", "min", min),
                ParameterUtils.validate("notBetween", "max", max)
        );
    }

    @Override
    default @NotNull Predicate notBetween(@NotNull T min, @NotNull T max) {
        return new BetweenPredicate(
                true,
                this,
                Literals.any(
                        ParameterUtils.validate("notBetween", "min", min)
                ),
                Literals.any(
                        ParameterUtils.validate("notBetween", "max", max)
                )
        );
    }

    @Override
    @NotNull
    default ComparableExpression<T> min() {
        return new AggregationExpression.Min<>(this);
    }

    @Override
    @NotNull
    default ComparableExpression<T> max() {
        return new AggregationExpression.Max<>(this);
    }

    @Override
    default @NotNull ComparableExpression<T> coalesce(T defaultValue) {
        return coalesceBuilder().or(defaultValue).build();
    }

    @Override
    default @NotNull ComparableExpression<T> coalesce(Expression<T> defaultExpr) {
        return coalesceBuilder().or(defaultExpr).build();
    }

    @Override
    default CoalesceBuilder.@NotNull Cmp<T> coalesceBuilder() {
        return new CoalesceBuilder.Cmp<>(this);
    }
}
