package org.babyfish.jimmer.sql.ast;

import org.babyfish.jimmer.EmbeddableDto;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ast.impl.*;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.jetbrains.annotations.NotNull;

import java.time.temporal.Temporal;
import java.util.Date;

public interface PropExpression<T> extends Expression<T> {

    interface Str extends PropExpression<String>, StringExpressionImplementor {
    }

    interface Num<N extends Number & Comparable<N>> extends PropExpression<N>, NumericExpressionImplementor<N> {
    }

    interface Dt<T extends Date & Comparable<Date>> extends PropExpression<T>, DateExpressionImplementor<T> {
    }

    interface Tp<T extends Temporal & Comparable<?>> extends PropExpression<T>, TemporalExpressionImplementor<T> {
    }

    interface Cmp<T extends Comparable<?>> extends PropExpression<T>, ComparableExpressionImplementor<T> {
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
