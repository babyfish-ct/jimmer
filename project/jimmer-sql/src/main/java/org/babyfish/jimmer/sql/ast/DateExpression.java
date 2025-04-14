package org.babyfish.jimmer.sql.ast;

import org.babyfish.jimmer.lang.NewChain;
import org.babyfish.jimmer.sql.ast.impl.CoalesceBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

public interface DateExpression<T extends Date & Comparable<Date>> extends ComparableExpression<T> {

    @NewChain
    DateExpression<T> plus(Expression<Long> value, SqlTimeUnit timeUnit);

    @NewChain
    default DateExpression<T> plus(long value, SqlTimeUnit timeUnit) {
        return plus(Expression.value(value), timeUnit);
    }

    @NewChain
    default DateExpression<T> minus(Expression<Long> value, SqlTimeUnit timeUnit) {
        if (value instanceof NumericExpression<?>) {
            return plus(((NumericExpression<Long>) value).unaryMinus(), timeUnit);
        }
        return plus(Expression.numeric().sql(Long.class, "-%e", value), timeUnit);
    }

    @NewChain
    default DateExpression<T> minus(long value, SqlTimeUnit timeUnit) {
        return plus(-value, timeUnit);
    }

    @NewChain
    NumericExpression<Long> diff(DateExpression<T> other, SqlTimeUnit timeUnit);

    @NewChain
    default NumericExpression<Long> diff(T other, SqlTimeUnit timeUnit) {
        return diff(Expression.value(other), timeUnit);
    }

    @Override
    @NotNull
    DateExpression<T> coalesce(T defaultValue);

    @Override
    @NotNull
    DateExpression<T> coalesce(Expression<T> defaultExpr);

    @Override
    @NotNull
    CoalesceBuilder.Dt<T> coalesceBuilder();
}
