package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.ast.NumericExpression;
import org.babyfish.jimmer.sql.ast.mutation.UserOptimisticLock;

public class OptimisticLockValueFactoryFactories<E> implements UserOptimisticLock.ValueExpressionFactory<E> {

    private static final OptimisticLockValueFactoryFactories<Object> FACTORY =
            new OptimisticLockValueFactoryFactories<>();

    @SuppressWarnings("unchecked")
    public static <E> UserOptimisticLock.ValueExpressionFactory<E> of() {
        return (UserOptimisticLock.ValueExpressionFactory<E>) FACTORY;
    }

    @Override
    public <N extends Number & Comparable<N>> NumericExpression<N> newValue(TypedProp.Scalar<E, N> prop) {
        return new OptimisticLockNewValueExpression<>(prop);
    }
}
