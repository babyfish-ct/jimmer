package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.Expression;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SqlExpressionContext {

    private List<Expression<?>> expressions = new ArrayList<>();

    private List<Object> values = new ArrayList<>();

    public SqlExpressionContext expression(Expression<?> expression) {
        Objects.requireNonNull(expression, "expression cannot be null");
        expressions.add(expression);
        return this;
    }

    public SqlExpressionContext value(Object value) {
        Objects.requireNonNull(value, "value cannot be null");
        if (value instanceof Expression<?>) {
            throw new IllegalArgumentException(
                    "value() cannot accept expression, please call expression()"
            );
        }
        values.add(value);
        return this;
    }

    List<Expression<?>> getExpressions() {
        return expressions;
    }

    List<Object> getValues() {
        return values;
    }
}
