package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.ComparableExpression;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;

interface ComparableExpressionImplementor<T extends Comparable<?>> extends ComparableExpression<T>, ExpressionImplementor<T> {

    @Override
    default Predicate lt(Expression<T> other) {
        return new ComparisonPredicate.Lt(this, other);
    }

    @Override
    default Predicate lt(T other) {
        return new ComparisonPredicate.Lt(this, Expression.comparable().value(other));
    }

    @Override
    default Predicate le(Expression<T> other) {
        return new ComparisonPredicate.Le(this, other);
    }

    @Override
    default Predicate le(T other) {
        return new ComparisonPredicate.Le(this, Expression.comparable().value(other));
    }

    @Override
    default Predicate gt(Expression<T> other) {
        return new ComparisonPredicate.Gt(this, other);
    }

    @Override
    default Predicate gt(T other) {
        return new ComparisonPredicate.Gt(this, Expression.comparable().value(other));
    }

    @Override
    default Predicate ge(Expression<T> other) {
        return new ComparisonPredicate.Ge(this, other);
    }

    @Override
    default Predicate ge(T other) {
        return new ComparisonPredicate.Ge(this, Expression.comparable().value(other));
    }

    @Override
    default Predicate between(Expression<T> min, Expression<T> max) {
        return new BetweenPredicate(false, this, min, max);
    }

    @Override
    default Predicate between(T min, T max) {
        return new BetweenPredicate(false, this, Literals.any(min), Literals.any(max));
    }

    @Override
    default Predicate notBetween(Expression<T> min, Expression<T> max) {
        return new BetweenPredicate(true, this, min, max);
    }

    @Override
    default Predicate notBetween(T min, T max) {
        return new BetweenPredicate(true, this, Literals.any(min), Literals.any(max));
    }

    @Override
    default ComparableExpression<T> coalesce(T defaultValue) {
        return coalesceBuilder().or(defaultValue).build();
    }

    @Override
    default ComparableExpression<T> coalesce(Expression<T> defaultExpr) {
        return coalesceBuilder().or(defaultExpr).build();
    }

    @Override
    default CoalesceBuilder.Cmp<T> coalesceBuilder() {
        return new CoalesceBuilder.Cmp<>(this);
    }
}
