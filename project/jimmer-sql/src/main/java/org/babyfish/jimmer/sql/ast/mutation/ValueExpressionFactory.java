package org.babyfish.jimmer.sql.ast.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.ast.ComparableExpression;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.NumericExpression;
import org.babyfish.jimmer.sql.ast.StringExpression;

/**
 * Creates scalar expressions for values supplied by the object being saved.
 * The physical SQL form can be a variable or a dialect-specific source column.
 */
public interface ValueExpressionFactory<E> {

    <V> Expression<V> newValue(TypedProp.Scalar<E, V> prop);

    StringExpression newString(TypedProp.Scalar<E, String> prop);

    <N extends Number & Comparable<N>> NumericExpression<N> newNumber(TypedProp.Scalar<E, N> prop);

    <C extends Comparable<?>> ComparableExpression<C> newComparable(TypedProp.Scalar<E, C> prop);

    <V> Expression<V> newValue(ImmutableProp prop);
}
