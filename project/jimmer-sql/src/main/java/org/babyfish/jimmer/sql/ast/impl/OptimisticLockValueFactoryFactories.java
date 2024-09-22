package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.ast.ComparableExpression;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.NumericExpression;
import org.babyfish.jimmer.sql.ast.StringExpression;
import org.babyfish.jimmer.sql.ast.mutation.UserOptimisticLock;

public class OptimisticLockValueFactoryFactories<E> implements UserOptimisticLock.ValueExpressionFactory<E> {

    private static final OptimisticLockValueFactoryFactories<Object> FACTORY =
            new OptimisticLockValueFactoryFactories<>();

    @SuppressWarnings("unchecked")
    public static <E> UserOptimisticLock.ValueExpressionFactory<E> of() {
        return (UserOptimisticLock.ValueExpressionFactory<E>) FACTORY;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V> Expression<V> newValue(TypedProp.Scalar<E, V> prop) {
        return newValue(prop.unwrap());
    }

    @Override
    public StringExpression newString(TypedProp.Scalar<E, String> prop) {
        return new OptimisticLockNewValueExpression.Str(prop.unwrap());
    }

    @Override
    public <N extends Number & Comparable<N>> NumericExpression<N> newNumber(TypedProp.Scalar<E, N> prop) {
        return new OptimisticLockNewValueExpression.Num<>(prop.unwrap());
    }

    @Override
    public <C extends Comparable<?>> ComparableExpression<C> newComparable(TypedProp.Scalar<E, C> prop) {
        return new OptimisticLockNewValueExpression.Cmp<>(prop.unwrap());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V> Expression<V> newValue(ImmutableProp prop) {
        Class<?> returnType = prop.getReturnClass();
        if (returnType == String.class) {
            return (Expression<V>) new OptimisticLockNewValueExpression.Str(prop);
        }
        if (Number.class.isAssignableFrom(returnType)) {
            return (Expression<V>) new OptimisticLockNewValueExpression.Num<>(prop);
        }
        if (Comparable.class.isAssignableFrom(returnType)) {
            return (Expression<V>) new OptimisticLockNewValueExpression.Cmp<>(prop);
        }
        return new OptimisticLockNewValueExpression<>(prop);
    }
}
