package org.babyfish.jimmer.sql.ast;

import org.babyfish.jimmer.lang.NewChain;
import org.babyfish.jimmer.sql.ast.impl.CoalesceBuilder;
import org.jetbrains.annotations.NotNull;

import java.time.temporal.Temporal;

public interface TemporalExpression<T extends Temporal &Comparable<?>> extends ComparableExpression<T> {

    @NewChain
    TemporalExpression<T> plus(Expression<Long> value, SqlTimeUnit timeUnit);

    @NewChain
    default TemporalExpression<T> plus(long value, SqlTimeUnit timeUnit) {
        return plus(Expression.value(value), timeUnit);
    }

    @NewChain
    default TemporalExpression<T> minus(Expression<Long> value, SqlTimeUnit timeUnit) {
        if (value instanceof NumericExpression<?>) {
            return plus(((NumericExpression<Long>) value).unaryMinus(), timeUnit);
        }
        return plus(Expression.numeric().sql(Long.class, "-%e", value), timeUnit);
    }

    @NewChain
    default TemporalExpression<T> minus(long value, SqlTimeUnit timeUnit) {
        return plus(-value, timeUnit);
    }

    NumericExpression<Long> diff(TemporalExpression<T> other, SqlTimeUnit timeUnit);

    default NumericExpression<Long> diff(T other, SqlTimeUnit timeUnit) {
        return diff(Expression.value(other), timeUnit);
    }

    @Override
    @NotNull
    TemporalExpression<T> coalesce(T defaultValue);

    @Override
    @NotNull
    TemporalExpression<T> coalesce(Expression<T> defaultExpr);

    @Override
    @NotNull
    CoalesceBuilder.Tp<T> coalesceBuilder();
}
