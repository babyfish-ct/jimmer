package org.babyfish.jimmer.sql.ast.query;

import org.babyfish.jimmer.lang.NewChain;
import org.babyfish.jimmer.sql.ast.Expression;

import java.util.Objects;

public class Order {

    private final Expression<?> expression;

    private final OrderMode orderMode;

    private final NullOrderMode nullOrderMode;

    public Order(Expression<?> expression, OrderMode orderMode, NullOrderMode nullOrderMode) {
        this.expression = Objects.requireNonNull(expression);
        this.orderMode = Objects.requireNonNull(orderMode);
        this.nullOrderMode = Objects.requireNonNull(nullOrderMode);
    }

    public Expression<?> getExpression() {
        return expression;
    }

    public OrderMode getOrderMode() {
        return orderMode;
    }

    public NullOrderMode getNullOrderMode() {
        return nullOrderMode;
    }

    @NewChain
    public Order nullsFirst() {
        return new Order(expression, orderMode, NullOrderMode.NULLS_FIRST);
    }

    @NewChain
    public Order nullsLast() {
        return new Order(expression, orderMode, NullOrderMode.NULLS_LAST);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return expression.equals(order.expression) && orderMode == order.orderMode && nullOrderMode == order.nullOrderMode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(expression, orderMode, nullOrderMode);
    }

    @Override
    public String toString() {
        return "Order{" +
                "expression=" + expression +
                ", orderMode=" + orderMode +
                ", nullOrderMode=" + nullOrderMode +
                '}';
    }
}
