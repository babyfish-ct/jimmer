package org.babyfish.jimmer.sql.ast;

import org.babyfish.jimmer.sql.ast.impl.CoalesceBuilder;

public interface PropExpression<T> extends Expression<T> {

    interface Str extends PropExpression<String>, StringExpression {

        @Override
        StringExpression coalesce(String defaultValue);

        @Override
        CoalesceBuilder.Str coalesceBuilder();
    }

    interface Num<N extends Number> extends PropExpression<N>, NumericExpression<N> {

        @Override
        NumericExpression<N> coalesce(N defaultValue);

        @Override
        CoalesceBuilder.Num<N> coalesceBuilder();
    }

    interface Cmp<T extends Comparable<T>> extends PropExpression<T>, ComparableExpression<T> {

        @Override
        ComparableExpression<T> coalesce(T defaultValue);

        @Override
        CoalesceBuilder.Cmp<T> coalesceBuilder();
    }
}
