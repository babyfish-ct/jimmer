package org.babyfish.jimmer.sql.ast;

public interface NumericExpression<N extends Number> extends Expression<N> {

    NumericExpression<N> plus(NumericExpression<N> other);

    NumericExpression<N> plus(N other);

    NumericExpression<N> minus(NumericExpression<N> other);

    NumericExpression<N> minus(N other);

    NumericExpression<N> prod(NumericExpression<N> other);

    NumericExpression<N> prod(N other);

    NumericExpression<N> div(NumericExpression<N> other);

    NumericExpression<N> div(N other);

    NumericExpression<N> rem(NumericExpression<N> other);

    NumericExpression<N> rem(N other);

    Predicate lt(NumericExpression<N> other);

    Predicate lt(N other);

    Predicate le(NumericExpression<N> other);

    Predicate le(N other);

    Predicate gt(NumericExpression<N> other);

    Predicate gt(N other);

    Predicate ge(NumericExpression<N> other);

    Predicate ge(N other);

    NumericExpression<N> sum();

    NumericExpression<N> avg();
}
