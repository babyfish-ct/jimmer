package org.babyfish.jimmer.sql.ast;

import org.babyfish.jimmer.sql.ast.impl.CoalesceBuilder;

public interface PropExpression<T> extends Expression<T> {

    interface Str extends PropExpression<String>, StringExpression {

        @Override
        StringExpression coalesce(String defaultValue);

        @Override
        StringExpression coalesce(Expression<String> defaultExpr);

        @Override
        CoalesceBuilder.Str coalesceBuilder();
    }

    interface Num<N extends Number> extends PropExpression<N>, NumericExpression<N> {

        @Override
        NumericExpression<N> coalesce(N defaultValue);

        @Override
        NumericExpression<N> coalesce(Expression<N> defaultExpr);

        @Override
        CoalesceBuilder.Num<N> coalesceBuilder();
    }

    interface Cmp<T extends Comparable<?>> extends PropExpression<T>, ComparableExpression<T> {

        @Override
        ComparableExpression<T> coalesce(T defaultValue);

        @Override
        ComparableExpression<T> coalesce(Expression<T> defaultExpr);

        @Override
        CoalesceBuilder.Cmp<T> coalesceBuilder();
    }
}
