package org.babyfish.jimmer.sql.ast;

import org.babyfish.jimmer.sql.ast.impl.CoalesceBuilder;

public interface ComparableExpression<T extends Comparable<T>> extends Expression<T> {

    Predicate lt(ComparableExpression<T> other);

    Predicate lt(T other);

    Predicate le(ComparableExpression<T> other);

    Predicate le(T other);

    Predicate gt(ComparableExpression<T> other);

    Predicate gt(T other);

    Predicate ge(ComparableExpression<T> other);

    Predicate ge(T other);

    @Override
    ComparableExpression<T> coalesce(T defaultValue);

    @Override
    CoalesceBuilder.Cmp<T> coalesceBuilder();
}
