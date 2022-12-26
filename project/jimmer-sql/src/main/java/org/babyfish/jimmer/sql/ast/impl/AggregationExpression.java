package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;

abstract class AggregationExpression<T> extends AbstractExpression<T> {

    Expression<?> expression;

    public AggregationExpression(Expression<?> expression) {
        this.expression = expression;
    }

    protected abstract String functionName();

    protected String prefix() {
        return null;
    }

    @Override
    public void accept(@NotNull AstVisitor visitor) {
        visitor.visitAggregation(functionName(), expression, prefix());
    }

    @Override
    public void renderTo(@NotNull SqlBuilder builder) {
        builder.sql(functionName());
        builder.sql("(");
        String prefix = prefix();
        if (prefix != null) {
            builder.sql(prefix);
            builder.sql(" ");
        }
        renderChild((Ast) expression, builder);
        builder.sql(")");
    }

    @Override
    public int precedence() {
        return 0;
    }

    static class Count extends AggregationExpression<Long> implements NumericExpressionImplementor<Long> {

        public Count(Expression<?> expression) {
            super(expression);
        }

        @Override
        protected String functionName() {
            return "count";
        }

        @Override
        public Class<Long> getType() {
            return Long.class;
        }
    }

    static class CountDistinct extends Count {

        public CountDistinct(Expression<?> expression) {
            super(expression);
        }

        @Override
        protected String prefix() {
            return "distinct";
        }
    }

    static class Sum<N extends Number & Comparable<N>> extends AggregationExpression<N> implements NumericExpressionImplementor<N> {

        public Sum(Expression<?> expression) {
            super(expression);
        }

        @Override
        protected String functionName() {
            return "sum";
        }

        @SuppressWarnings("unchecked")
        @Override
        public Class<N> getType() {
            return (Class<N>)((AbstractExpression<?>)expression).getType();
        }
    }

    static class Min<N extends Number & Comparable<N>> extends AggregationExpression<N> implements NumericExpressionImplementor<N> {

        public Min(Expression<?> expression) {
            super(expression);
        }

        @Override
        protected String functionName() {
            return "min";
        }

        @SuppressWarnings("unchecked")
        @Override
        public Class<N> getType() {
            return (Class<N>)((AbstractExpression<?>)expression).getType();
        }
    }

    static class Max<N extends Number & Comparable<N>> extends AggregationExpression<N> implements NumericExpressionImplementor<N> {

        public Max(Expression<?> expression) {
            super(expression);
        }

        @Override
        protected String functionName() {
            return "max";
        }

        @SuppressWarnings("unchecked")
        @Override
        public Class<N> getType() {
            return (Class<N>)((AbstractExpression<?>)expression).getType();
        }
    }

    static class Avg extends AggregationExpression<BigDecimal> implements NumericExpressionImplementor<BigDecimal> {

        public Avg(Expression<? extends Number> expression) {
            super(expression);
        }

        @Override
        protected String functionName() {
            return "avg";
        }

        @SuppressWarnings("unchecked")
        @Override
        public Class<BigDecimal> getType() {
            return BigDecimal.class;
        }
    }
}
