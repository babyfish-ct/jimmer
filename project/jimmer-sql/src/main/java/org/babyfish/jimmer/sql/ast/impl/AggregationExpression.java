package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.PropExpression;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.table.spi.PropExpressionImplementor;
import org.babyfish.jimmer.sql.meta.EmbeddedColumns;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.Objects;

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
    public final void renderTo(@NotNull AbstractSqlBuilder<?> builder) {
        validate(builder.sqlClient());

        builder.sql(functionName());
        builder.sql("(");
        String prefix = prefix();
        if (prefix != null) {
            builder.sql(prefix);
            builder.sql(" ");
        }
        renderExpression(builder);
        builder.sql(")");
    }

    @Override
    protected boolean determineHasVirtualPredicate() {
        return hasVirtualPredicate(expression);
    }

    @Override
    protected Ast onResolveVirtualPredicate(AstContext ctx) {
        this.expression = ctx.resolveVirtualPredicate(expression);
        return this;
    }

    private void validate(JSqlClientImplementor sqlClient) {
        if (!sqlClient.getDialect().isTupleCountSupported()) {
            if (expression instanceof PropExpression<?>) {
                PropExpressionImpl<?> propExpr = (PropExpressionImpl<?>) expression;
                EmbeddedColumns.Partial partial = propExpr.getPartial(sqlClient.getMetadataStrategy());
                if (partial != null && partial.size() > 1) {
                    throw new IllegalArgumentException(
                            "The `count` function does not support embedded property " +
                                    "because multiple columns `count` is not supported by current dialect \"" +
                                    sqlClient.getDialect().getClass().getName() +
                                    "\""
                    );
                }
            } else if (expression instanceof TupleExpressionImplementor<?>) {
                throw new IllegalArgumentException(
                        "The `count` function does not support tuple expression " +
                                "because multiple columns `count` is not supported by current dialect \"" +
                                sqlClient.getDialect().getClass().getName() +
                                "\""
                );
            }
        }
    }

    protected void renderExpression(@NotNull AbstractSqlBuilder<?> builder) {
        renderChild((Ast) expression, builder);
    }

    @Override
    public int precedence() {
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AggregationExpression<?> that = (AggregationExpression<?>) o;
        return expression.equals(that.expression);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expression);
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

        @Override
        protected void renderExpression(@NotNull AbstractSqlBuilder<?> builder) {
            if (builder.sqlClient().getDialect().isTupleCountSupported()) {
                if (expression instanceof PropExpressionImplementor<?>) {
                    ((PropExpressionImplementor<?>) expression).renderTo(builder, true);
                    return;
                }
                if (expression instanceof TupleExpressionImplementor<?>) {
                    ((TupleExpressionImplementor<?>) expression).renderTo(builder, true);
                    return;
                }
            }
            super.renderExpression(builder);
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

    static class SumAsLong extends AggregationExpression<Long> implements NumericExpressionImplementor<Long> {

        public SumAsLong(Expression<?> expression) {
            super(expression);
        }

        @Override
        protected String functionName() {
            return "sum";
        }

        @SuppressWarnings("unchecked")
        @Override
        public Class<Long> getType() {
            return Long.class;
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

    static class Avg extends AggregationExpression<Double> implements NumericExpressionImplementor<Double> {

        public Avg(Expression<? extends Number> expression) {
            super(expression);
        }

        @Override
        protected String functionName() {
            return "avg";
        }

        @SuppressWarnings("unchecked")
        @Override
        public Class<Double> getType() {
            return Double.class;
        }
    }

    static class AvgAsDecimal extends AggregationExpression<BigDecimal> implements NumericExpressionImplementor<BigDecimal> {

        public AvgAsDecimal(Expression<? extends Number> expression) {
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
