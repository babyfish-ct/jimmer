package org.babyfish.jimmer.sql.ast;

import org.babyfish.jimmer.EmbeddableDto;
import org.babyfish.jimmer.View;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ast.impl.CoalesceBuilder;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.jetbrains.annotations.NotNull;

import java.time.temporal.Temporal;
import java.util.Date;

public interface PropExpression<T> extends Expression<T> {

    interface Str extends PropExpression<String>, StringExpression {

        @Override
        @NotNull
        StringExpression coalesce(String defaultValue);

        @Override
        @NotNull
        StringExpression coalesce(Expression<String> defaultExpr);

        @Override
        CoalesceBuilder.@NotNull Str coalesceBuilder();
    }

    interface Num<N extends Number & Comparable<N>> extends PropExpression<N>, NumericExpression<N> {

        @Override
        @NotNull
        NumericExpression<N> coalesce(N defaultValue);

        @Override
        @NotNull
        NumericExpression<N> coalesce(Expression<N> defaultExpr);

        @Override
        @NotNull
        CoalesceBuilder.Num<N> coalesceBuilder();
    }

    interface Dt<T extends Date & Comparable<Date>> extends PropExpression<T>, DateExpression<T> {

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

    interface Tp<T extends Temporal & Comparable<?>> extends PropExpression<T>, TemporalExpression<T> {

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

    interface Cmp<T extends Comparable<?>> extends PropExpression<T>, ComparableExpression<T> {

        @Override
        @NotNull
        ComparableExpression<T> coalesce(T defaultValue);

        @Override
        @NotNull
        ComparableExpression<T> coalesce(Expression<T> defaultExpr);

        @Override
        CoalesceBuilder.@NotNull Cmp<T> coalesceBuilder();
    }

    interface Embedded<T> extends PropExpression<T> {

        <XE extends Expression<?>> XE get(String prop);

        <XE extends Expression<?>> XE get(ImmutableProp prop);

        Selection<T> fetch(Fetcher<T> fetcher);

        <D extends EmbeddableDto<T>> Selection<D> fetch(Class<D> dtoType);

        @Override
        default @NotNull Expression<T> coalesce(T defaultValue) {
            throw new UnsupportedOperationException("Embedded property does not support coalesce");
        }

        @Override
        default @NotNull Expression<T> coalesce(Expression<T> defaultExpr) {
            throw new UnsupportedOperationException("Embedded property does not support coalesce");
        }

        @Override
        default @NotNull CoalesceBuilder<T> coalesceBuilder() {
            throw new UnsupportedOperationException("Embedded property does not support coalesce");
        }
    }
}
