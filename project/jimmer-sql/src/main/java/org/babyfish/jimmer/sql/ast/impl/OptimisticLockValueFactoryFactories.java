package org.babyfish.jimmer.sql.ast.impl;

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
        Class<?> returnType = prop.unwrap().getReturnClass();
        if (returnType == String.class) {
            return (Expression<V>) new OptimisticLockNewValueExpression.Str((TypedProp.Scalar<E, String>)prop);
        }
        if (Number.class.isAssignableFrom(returnType)) {
            return (Expression<V>) new OptimisticLockNewValueExpression.Num<>((TypedProp.Scalar<E, Long>)prop);
        }
        if (Comparable.class.isAssignableFrom(returnType)) {
            return (Expression<V>) new OptimisticLockNewValueExpression.Cmp<>((TypedProp.Scalar<E, Comparable<?>>)prop);
        }
        return new OptimisticLockNewValueExpression<>(prop);
    }

    @Override
    public StringExpression newString(TypedProp.Scalar<E, String> prop) {
        return new OptimisticLockNewValueExpression.Str(prop);
    }

    @Override
    public <N extends Number & Comparable<N>> NumericExpression<N> newNumber(TypedProp.Scalar<E, N> prop) {
        return new OptimisticLockNewValueExpression.Num<>(prop);
    }

    @Override
    public <C extends Comparable<?>> ComparableExpression<C> newComparable(TypedProp.Scalar<E, C> prop) {
        return new OptimisticLockNewValueExpression.Cmp<>(prop);
    }
}
