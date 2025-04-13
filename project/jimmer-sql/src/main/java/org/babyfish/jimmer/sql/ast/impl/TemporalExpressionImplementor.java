package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.NumericExpression;
import org.babyfish.jimmer.sql.ast.SqlTimeUnit;
import org.babyfish.jimmer.sql.ast.TemporalExpression;
import org.jetbrains.annotations.NotNull;

import java.time.temporal.Temporal;

public interface TemporalExpressionImplementor<T extends Temporal & Comparable<?>>
        extends ComparableExpressionImplementor<T>, TemporalExpression<T> {

    @Override
    default TemporalExpression<T> plus(Expression<Long> value, SqlTimeUnit timeUnit) {
        return new TemporalPlusExpression<>(this, value, timeUnit);
    }

    @Override
    default NumericExpression<Long> diff(TemporalExpression<T> other, SqlTimeUnit timeUnit) {
        return new TemporalDiffExpression<>(this, other, timeUnit);
    }

    @Override
    @NotNull
    default TemporalExpression<T> coalesce(Expression<T> defaultExpr) {
        return coalesceBuilder().or(defaultExpr).build();
    }

    @Override
    @NotNull
    default TemporalExpression<T> coalesce(T defaultValue) {
        return coalesceBuilder().or(defaultValue).build();
    }

    @Override
    @NotNull
    default CoalesceBuilder.Tp<T> coalesceBuilder() {
        return new CoalesceBuilder.Tp<>(this);
    }
}
