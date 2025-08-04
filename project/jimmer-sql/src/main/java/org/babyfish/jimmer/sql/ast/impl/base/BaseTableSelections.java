package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.sql.ast.*;
import org.babyfish.jimmer.sql.ast.embedded.AbstractTypedEmbeddedPropExpression;
import org.babyfish.jimmer.sql.ast.impl.*;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.table.BaseTable;
import org.babyfish.jimmer.sql.ast.table.spi.PropExpressionImplementor;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;

import java.time.temporal.Temporal;
import java.util.Date;

public class BaseTableSelections {

    private BaseTableSelections() {}

    @SuppressWarnings("unchecked")
    public static <T extends Selection<?>> T of(
            T selection,
            BaseTable baseTable,
            int index
    ) {
        if (selection instanceof TableProxy<?>) {
            return (T) of((TableProxy<?>) selection, baseTable, index);
        }
        if (selection instanceof TableImplementor<?>) {
            return (T) of((TableImplementor<?>) selection, baseTable, index);
        }
        if (selection instanceof Expression<?>) {
            return (T) of((Expression<?>) selection, baseTable, index);
        }
        throw new IllegalArgumentException("base query can only select table or expression");
    }

    @SuppressWarnings("unchecked")
    public static <T extends TableProxy<?>> T of(
            T table,
            BaseTable baseTable,
            int index
    ) {
        return (T) table.__baseTableOwner(new BaseTableOwner(baseTable, index));
    }

    public static TableImplementor<?> of(
            TableImplementor<?> table,
            BaseTable baseTable,
            int index
    ) {
        return table.baseTableOwner(new BaseTableOwner(baseTable, index));
    }

    @SuppressWarnings("unchecked")
    public static <T> Expression<T> of(
            Expression<T> expr,
            BaseTable baseTable,
            int index
    ) {
//        if (expr instanceof PropExpression<?>) {
//            return of((PropExpression<T>) expr, baseTable, index);
//        }
        if (expr instanceof ComparableExpression<?>) {
            return (Expression<T>) of((ComparableExpression<?>)expr, baseTable, index);
        }
        return new BaseTableExpression<>((ExpressionImplementor<T>) expr, new BaseTableOwner(baseTable, index));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T extends Comparable<?>> ComparableExpression<T> of(
            ComparableExpression<T> expr,
            BaseTable baseTable,
            int index
    ) {
//        if (expr instanceof PropExpression<?>) {
//            return of((PropExpression.Cmp<T>) expr, baseTable, index);
//        }
        BaseTableOwner owner = new BaseTableOwner(baseTable, index);
        if (expr instanceof StringExpression) {
            return (ComparableExpression<T>) new BaseTableExpression.Str((StringExpressionImplementor) expr, owner);
        }
        if (expr instanceof NumericExpression<?>) {
            return (ComparableExpression<T>) new BaseTableExpression.Num((NumericExpressionImplementor) expr, owner);
        }
        if (expr instanceof DateExpression<?>) {
            return new BaseTableExpression.Dt<>((DateExpressionImplementor) expr, owner);
        }
        if (expr instanceof TemporalExpression<?>) {
            return new BaseTableExpression.Tp<>((TemporalExpressionImplementor) expr, owner);
        }
        return new BaseTableExpression.Cmp<>((ComparableExpressionImplementor<T>) expr, owner);
    }

    @SuppressWarnings("unchecked")
    public static StringExpression of(
            StringExpression expr,
            BaseTable baseTable,
            int index
    ) {
        BaseTableOwner owner = new BaseTableOwner(baseTable, index);
//        if (expr instanceof PropExpression.Str) {
//            return new BaseTablePropExpression.Str((PropExpressionImplementor<String>) expr, owner);
//        }
        return new BaseTableExpression.Str((StringExpressionImplementor) expr, owner);
    }

    @SuppressWarnings("unchecked")
    public static <N extends Number & Comparable<N>> NumericExpression<N> of(
            NumericExpression<N> expr,
            BaseTable baseTable,
            int index
    ) {
        BaseTableOwner owner = new BaseTableOwner(baseTable, index);
//        if (expr instanceof PropExpression.Num<?>) {
//            return new BaseTablePropExpression.Num<>((PropExpressionImplementor<N>) expr, owner);
//        }
        return new BaseTableExpression.Num<>((NumericExpressionImplementor<N>) expr, owner);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Date> DateExpression<T> of(
            DateExpression<T> expr,
            BaseTable baseTable,
            int index
    ) {
        BaseTableOwner owner = new BaseTableOwner(baseTable, index);
//        if (expr instanceof PropExpression.Dt<?>) {
//            return new BaseTablePropExpression.Dt<>((PropExpressionImplementor<T>) expr, owner);
//        }
        return new BaseTableExpression.Dt<>((DateExpressionImplementor<T>) expr, owner);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Temporal & Comparable<?>> TemporalExpression<T> of(
            TemporalExpression<T> expr,
            BaseTable baseTable,
            int index
    ) {
        BaseTableOwner owner = new BaseTableOwner(baseTable, index);
//        if (expr instanceof PropExpression.Tp<?>) {
//            return new BaseTablePropExpression.Tp<>((PropExpressionImplementor<T>) expr, owner);
//        }
        return new BaseTableExpression.Tp<>((TemporalExpressionImplementor<T>) expr, owner);
    }

//    @SuppressWarnings("unchecked")
//    public static <T> PropExpression<T> of(
//            PropExpression<T> expr,
//            BaseTable baseTable,
//            int index
//    ) {
//        if (expr instanceof AbstractTypedEmbeddedPropExpression<?>) {
//            return (PropExpression<T>) ((AbstractTypedEmbeddedPropExpression<?>)expr)
//                    .__baseTableOwner(new BaseTableOwner(baseTable, index));
//        }
//        if (expr instanceof PropExpression.Cmp<?>) {
//            return (PropExpression<T>) of((PropExpression.Cmp<?>)expr, baseTable, index);
//        }
//        return new BaseTablePropExpression<>(
//                (PropExpressionImplementor<T>) expr,
//                new BaseTableOwner(baseTable, index)
//        );
//    }
//
//    @SuppressWarnings({"unchecked", "rawtypes"})
//    public static <T extends Comparable<?>> PropExpression.Cmp<T> of(
//            PropExpression.Cmp<T> expr,
//            BaseTable baseTable,
//            int index
//    ) {
//        BaseTableOwner owner = new BaseTableOwner(baseTable, index);
//        if (expr instanceof PropExpression.Str) {
//            return (PropExpression.Cmp<T>) new BaseTablePropExpression.Str((PropExpressionImplementor<String>) expr, owner);
//        }
//        if (expr instanceof NumericExpression<?>) {
//            return new BaseTablePropExpression.Num((PropExpressionImplementor) expr, owner);
//        }
//        if (expr instanceof DateExpression<?>) {
//            return new BaseTablePropExpression.Dt<>((PropExpressionImplementor) expr, owner);
//        }
//        if (expr instanceof TemporalExpression<?>) {
//            return new BaseTablePropExpression.Tp<>((PropExpressionImplementor) expr, owner);
//        }
//        return new BaseTablePropExpression.Cmp<>((PropExpressionImplementor<T>) expr, owner);
//    }
//
//    @SuppressWarnings("unchecked")
//    public static PropExpression.Str of(
//            PropExpression.Str expr,
//            BaseTable baseTable,
//            int index
//    ) {
//        return new BaseTablePropExpression.Str(
//                (PropExpressionImplementor<String>) expr,
//                new BaseTableOwner(baseTable, index)
//        );
//    }
//
//    @SuppressWarnings("unchecked")
//    public static <N extends Number & Comparable<N>> PropExpression.Cmp<N> of(
//            PropExpression.Num<N> expr,
//            BaseTable baseTable,
//            int index
//    ) {
//        return new BaseTablePropExpression.Num<>(
//                (PropExpressionImplementor<N>) expr,
//                new BaseTableOwner(baseTable, index)
//        );
//    }
//
//    @SuppressWarnings("unchecked")
//    public static <T extends Date> PropExpression.Dt<T> of(
//            PropExpression.Dt<T> expr,
//            BaseTable baseTable,
//            int index
//    ) {
//        new BaseTableOwner(baseTable, index);
//        return new BaseTablePropExpression.Dt<>(
//                (PropExpressionImplementor<T>) expr,
//                new BaseTableOwner(baseTable, index)
//        );
//    }
//
//    @SuppressWarnings("unchecked")
//    public static <T extends Temporal & Comparable<?>> PropExpression.Tp<T> of(
//            PropExpression.Tp<T> expr,
//            BaseTable baseTable,
//            int index
//    ) {
//        return new BaseTablePropExpression.Tp<>(
//                (PropExpressionImplementor<T>) expr,
//                new BaseTableOwner(baseTable, index)
//        );
//    }
}
