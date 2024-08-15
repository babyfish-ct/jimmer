package org.babyfish.jimmer.sql.ast.mutation;

import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.ast.*;
import org.babyfish.jimmer.sql.ast.table.Table;

public interface UserOptimisticLock<E, T extends Table<E>> {

    Predicate predicate(T table, ValueExpressionFactory<E> valueExpressionFactory);

    interface ValueExpressionFactory<E> {
        <V> Expression<V> newValue(TypedProp.Scalar<E, V> prop);
        StringExpression newString(TypedProp.Scalar<E, String> prop);
        <N extends Number & Comparable<N>> NumericExpression<N> newNumber(TypedProp.Scalar<E, N> prop);
        <C extends Comparable<?>> ComparableExpression<C> newComparable(TypedProp.Scalar<E, C> prop);
    }
}
