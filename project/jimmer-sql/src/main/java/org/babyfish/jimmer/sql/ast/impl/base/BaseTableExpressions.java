package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.sql.ast.*;
import org.babyfish.jimmer.sql.ast.embedded.AbstractTypedEmbeddedPropExpression;
import org.babyfish.jimmer.sql.ast.impl.*;
import org.babyfish.jimmer.sql.ast.table.BaseTable;
import org.babyfish.jimmer.sql.ast.table.spi.PropExpressionImplementor;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;

import java.time.temporal.Temporal;
import java.util.Date;

public class BaseTableExpressions {

    private BaseTableExpressions() {}

    @SuppressWarnings("unchecked")
    public static <T extends TableProxy<?>> T of(
            T table,
            BaseTable<?> baseTable
    ) {
        return (T) table.__baseTableOwner(baseTable);
    }

    @SuppressWarnings("unchecked")
    public static <T extends AbstractTypedEmbeddedPropExpression<?>> T of(
            T expr,
            BaseTable<?> baseTable
    ) {
        return (T) expr.__baseTableOwner(baseTable);
    }

    @SuppressWarnings("unchecked")
    public <T> Expression<T> of(
            Expression<T> expr,
            BaseTable<?> baseTable
    ) {
        if (expr instanceof PropExpression<?>) {
            return of((PropExpression<T>) expr, baseTable);
        }
        if (expr instanceof ComparableExpression<?>) {
            return (Expression<T>) of((ComparableExpression<?>)expr, baseTable);
        }
        return new BaseTableExpression<>((ExpressionImplementor<T>) expr, baseTable);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T extends Comparable<?>> ComparableExpression<T> of(
            ComparableExpression<T> expr,
            BaseTable<?> baseTable
    ) {
        if (expr instanceof PropExpression<?>) {
            return of((PropExpression.Cmp<T>) expr, baseTable);
        }
        if (expr instanceof StringExpression) {
            return (ComparableExpression<T>) new BaseTableExpression.Str((StringExpressionImplementor) expr, baseTable);
        }
        if (expr instanceof NumericExpression<?>) {
            return (ComparableExpression<T>) new BaseTableExpression.Num((NumericExpressionImplementor) expr, baseTable);
        }
        if (expr instanceof DateExpression<?>) {
            return new BaseTableExpression.Dt<>((DateExpressionImplementor) expr, baseTable);
        }
        if (expr instanceof TemporalExpression<?>) {
            return new BaseTableExpression.Tp<>((TemporalExpressionImplementor) expr, baseTable);
        }
        return new BaseTableExpression.Cmp<>((ComparableExpressionImplementor<T>) expr, baseTable);
    }

    @SuppressWarnings("unchecked")
    public static StringExpression of(
            StringExpression expr,
            BaseTable<?> baseTable
    ) {
        if (expr instanceof PropExpression.Str) {
            return new BaseTablePropExpression.Str((PropExpressionImplementor<String>) expr, baseTable);
        }
        return new BaseTableExpression.Str((StringExpressionImplementor) expr, baseTable);
    }

    @SuppressWarnings("unchecked")
    public static <N extends Number & Comparable<N>> NumericExpression<N> of(
            NumericExpression<N> expr,
            BaseTable<?> baseTable
    ) {
        if (expr instanceof PropExpression.Num<?>) {
            return new BaseTablePropExpression.Num<>((PropExpressionImplementor<N>) expr, baseTable);
        }
        return new BaseTableExpression.Num<>((NumericExpressionImplementor<N>) expr, baseTable);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Date> DateExpression<T> of(
            DateExpression<T> expr,
            BaseTable<?> baseTable
    ) {
        if (expr instanceof PropExpression.Dt<?>) {
            return new BaseTablePropExpression.Dt<>((PropExpressionImplementor<T>) expr, baseTable);
        }
        return new BaseTableExpression.Dt<>((DateExpressionImplementor<T>) expr, baseTable);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Temporal & Comparable<?>> TemporalExpression<T> of(
            TemporalExpression<T> expr,
            BaseTable<?> baseTable
    ) {
        if (expr instanceof PropExpression.Tp<?>) {
            return new BaseTablePropExpression.Tp<>((PropExpressionImplementor<T>) expr, baseTable);
        }
        return new BaseTableExpression.Tp<>((TemporalExpressionImplementor<T>) expr, baseTable);
    }

    @SuppressWarnings("unchecked")
    public <T> PropExpression<T> of(
            PropExpression<T> expr,
            BaseTable<?> baseTable
    ) {
        if (expr instanceof PropExpression.Cmp<?>) {
            return (PropExpression<T>) of((PropExpression.Cmp<?>)expr, baseTable);
        }
        return new BaseTablePropExpression<>((PropExpressionImplementor<T>) expr, baseTable);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T extends Comparable<?>> PropExpression.Cmp<T> of(
            PropExpression.Cmp<T> expr,
            BaseTable<?> baseTable
    ) {
        if (expr instanceof PropExpression.Str) {
            return (PropExpression.Cmp<T>) new BaseTablePropExpression.Str((PropExpressionImplementor<String>) expr, baseTable);
        }
        if (expr instanceof NumericExpression<?>) {
            return new BaseTablePropExpression.Num((PropExpressionImplementor) expr, baseTable);
        }
        if (expr instanceof DateExpression<?>) {
            return new BaseTablePropExpression.Dt<>((PropExpressionImplementor) expr, baseTable);
        }
        if (expr instanceof TemporalExpression<?>) {
            return new BaseTablePropExpression.Tp<>((PropExpressionImplementor) expr, baseTable);
        }
        return new BaseTablePropExpression.Cmp<>((PropExpressionImplementor<T>) expr, baseTable);
    }

    @SuppressWarnings("unchecked")
    public static PropExpression.Str of(
            PropExpression.Str expr,
            BaseTable<?> baseTable
    ) {
        return new BaseTablePropExpression.Str((PropExpressionImplementor<String>) expr, baseTable);
    }

    @SuppressWarnings("unchecked")
    public static <N extends Number & Comparable<N>> PropExpression.Cmp<N> of(
            PropExpression.Num<N> expr,
            BaseTable<?> baseTable
    ) {
        return new BaseTablePropExpression.Num<>((PropExpressionImplementor<N>) expr, baseTable);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Date> PropExpression.Dt<T> of(
            PropExpression.Dt<T> expr,
            BaseTable<?> baseTable
    ) {
        return new BaseTablePropExpression.Dt<>((PropExpressionImplementor<T>) expr, baseTable);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Temporal & Comparable<?>> PropExpression.Tp<T> of(
            PropExpression.Tp<T> expr,
            BaseTable<?> baseTable
    ) {
        return new BaseTablePropExpression.Tp<>((PropExpressionImplementor<T>) expr, baseTable);
    }
}
