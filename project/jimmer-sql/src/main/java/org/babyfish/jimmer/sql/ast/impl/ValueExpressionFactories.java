package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.ast.ComparableExpression;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.NumericExpression;
import org.babyfish.jimmer.sql.ast.StringExpression;
import org.babyfish.jimmer.sql.ast.mutation.ValueExpressionFactory;

public final class ValueExpressionFactories<E> implements ValueExpressionFactory<E> {

    private static final ValueExpressionFactories<Object> FACTORY =
            new ValueExpressionFactories<>();

    private ValueExpressionFactories() {}

    @SuppressWarnings("unchecked")
    public static <E> ValueExpressionFactory<E> of() {
        return (ValueExpressionFactory<E>) FACTORY;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V> Expression<V> newValue(TypedProp.Scalar<E, V> prop) {
        return newValue(prop.unwrap());
    }

    @Override
    public StringExpression newString(TypedProp.Scalar<E, String> prop) {
        return new SaveInputValueExpression.Str(prop.unwrap());
    }

    @Override
    public <N extends Number & Comparable<N>> NumericExpression<N> newNumber(TypedProp.Scalar<E, N> prop) {
        return new SaveInputValueExpression.Num<>(prop.unwrap());
    }

    @Override
    public <C extends Comparable<?>> ComparableExpression<C> newComparable(TypedProp.Scalar<E, C> prop) {
        return new SaveInputValueExpression.Cmp<>(prop.unwrap());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V> Expression<V> newValue(ImmutableProp prop) {
        Class<?> returnType = prop.getReturnClass();
        if (returnType == String.class) {
            return (Expression<V>) new SaveInputValueExpression.Str(prop);
        }
        if (Number.class.isAssignableFrom(returnType)) {
            return (Expression<V>) new SaveInputValueExpression.Num<>(prop);
        }
        if (Comparable.class.isAssignableFrom(returnType)) {
            return (Expression<V>) new SaveInputValueExpression.Cmp<>(prop);
        }
        return new SaveInputValueExpression<>(prop);
    }
}
