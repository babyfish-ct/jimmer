package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.*;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

public interface DateExpressionImplementor<T extends Date & Comparable<Date>>
        extends ComparableExpressionImplementor<T>, DateExpression<T> {

    @Override
    default DateExpression<T> plus(Expression<Long> value, SqlTimeUnit timeUnit) {
        return new DataPlusExpression<>(this, value, timeUnit);
    }

    @Override
    default NumericExpression<Float> diff(DateExpression<T> other, SqlTimeUnit timeUnit) {
        return new DateDiffExpression<>(this, other, timeUnit);
    }

    @Override
    @NotNull
    default DateExpression<T> coalesce(Expression<T> defaultExpr) {
        return coalesceBuilder().or(defaultExpr).build();
    }

    @Override
    @NotNull
    default DateExpression<T> coalesce(T defaultValue) {
        return coalesceBuilder().or(defaultValue).build();
    }

    @Override
    @NotNull
    default CoalesceBuilder.Dt<T> coalesceBuilder() {
        return new CoalesceBuilder.Dt<>(this);
    }
}
