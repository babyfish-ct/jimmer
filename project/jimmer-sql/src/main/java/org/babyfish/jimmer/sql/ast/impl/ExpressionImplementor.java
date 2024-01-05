package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.NumericExpression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.query.NullOrderMode;
import org.babyfish.jimmer.sql.ast.query.Order;
import org.babyfish.jimmer.sql.ast.query.OrderMode;
import org.babyfish.jimmer.sql.ast.query.TypedSubQuery;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;

public interface ExpressionImplementor<T> extends Expression<T> {

    Class<T> getType();

    int precedence();

    @NotNull
    @Override
    default Predicate eq(@NotNull Expression<T> other) {
        if (other instanceof NullExpression<?>) {
            return isNull();
        } else if (this instanceof NullExpression<?>) {
            return other.isNull();
        }
        return new ComparisonPredicate.Eq(this, other);
    }

    @NotNull
    @Override
    default Predicate eq(@Nullable T other) {
        if (other == null) {
            return isNull();
        }
        return eq(Literals.any(other));
    }

    @NotNull
    @Override
    default Predicate ne(@NotNull Expression<T> other) {
        if (other instanceof NullExpression<?>) {
            return isNotNull();
        } else if (this instanceof NullExpression<?>) {
            return other.isNotNull();
        }
        return new ComparisonPredicate.Ne(this, other);
    }

    @NotNull
    @Override
    default Predicate ne(@Nullable T other) {
        if (other == null) {
            return isNotNull();
        }
        return ne(Literals.any(other));
    }

    @Override
    default @NotNull Predicate isNull() {
        return new NullityPredicate(this, false);
    }

    @Override
    default @NotNull Predicate isNotNull() {
        return new NullityPredicate(this, true);
    }

    @Override
    default @NotNull Predicate in(@NotNull Collection<T> values) {
        return new InCollectionPredicate(this, values, false);
    }

    @Override
    default @NotNull Predicate notIn(@NotNull Collection<T> values) {
        return new InCollectionPredicate(this, values, true);
    }

    @Override
    default @NotNull Predicate in(@NotNull TypedSubQuery<T> subQuery) {
        return new InSubQueryPredicate(this, subQuery, false);
    }

    @Override
    default @NotNull Predicate notIn(@NotNull TypedSubQuery<T> subQuery) {
        return new InSubQueryPredicate(this, subQuery, true);
    }

    @Override
    default @NotNull NumericExpression<Long> count() {
        return count(false);
    }

    @Override
    default @NotNull NumericExpression<Long> count(boolean distinct) {
        if (distinct) {
            return new AggregationExpression.CountDistinct(this);
        }
        return new AggregationExpression.Count(this);
    }

    @Override
    default @NotNull Expression<T> coalesce(T defaultValue) {
        return coalesceBuilder().or(defaultValue).build();
    }

    @Override
    default @NotNull Expression<T> coalesce(Expression<T> defaultExpr) {
        return coalesceBuilder().or(defaultExpr).build();
    }

    @Override
    default @NotNull CoalesceBuilder<T> coalesceBuilder() {
        return new CoalesceBuilder<>(this);
    }

    @Override
    default @NotNull Order asc() {
        return new Order(this, OrderMode.ASC, NullOrderMode.UNSPECIFIED);
    }

    @Override
    default @NotNull Order desc() {
        return new Order(this, OrderMode.DESC, NullOrderMode.UNSPECIFIED);
    }
}
