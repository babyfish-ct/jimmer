package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.ComparableExpression;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.NumericExpression;
import org.babyfish.jimmer.sql.ast.StringExpression;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class CoalesceBuilder<T> {

    private List<Expression<T>> expressions = new ArrayList<>();

    CoalesceBuilder(Expression<T> expression) {
        expressions.add(expression);
    }

    public CoalesceBuilder<T> or(Expression<T> expr) {
        if (((ExpressionImplementor<?>) expressions.get(0)).getType() !=
                ((ExpressionImplementor<?>)expr).getType()) {
            throw new IllegalArgumentException("The branches of coalesce must belong to same type");
        }
        expressions.add(expr);
        return this;
    }

    public CoalesceBuilder<T> or(T value) {
        return or(Literals.any(value));
    }

    @SuppressWarnings("unchecked")
    public Expression<T> build() {
        List<Expression<?>> clonedList;
        if (expressions.get(0) instanceof Expr<?>) {
            clonedList = new ArrayList<>(((Expr<?>)expressions.get(0)).expressions);
            clonedList.addAll(expressions.subList(1, expressions.size()));
        } else {
            clonedList = new ArrayList<>(expressions);
        }
        Class<?> javaClass = ((ExpressionImplementor<?>) expressions.get(0)).getType();
        if (String.class == javaClass) {
            return (Expression<T>) new StrExpr(clonedList);
        }
        if (javaClass.isPrimitive() || Number.class.isAssignableFrom(javaClass)) {
            return (Expression<T>) new NumExpr<>(clonedList);
        }
        if (Comparable.class.isAssignableFrom(javaClass)) {
            return (Expression<T>) new CmpExpr<>(clonedList);
        }
        return new Expr<>(clonedList);
    }

    public static class Str extends Cmp<String> {

        Str(Expression<String> expression) {
            super(expression);
        }

        @Override
        public Str or(Expression<String> expr) {
            return (Str) super.or(expr);
        }

        @Override
        public Str or(String value) {
            return (Str) super.or(value);
        }

        @Override
        public StringExpression build() {
            return (StringExpression) super.build();
        }
    }

    public static class Num<N extends Number & Comparable<N>> extends Cmp<N> {

        Num(Expression<N> expression) {
            super(expression);
        }

        @Override
        public Num<N> or(Expression<N> expr) {
            return (Num<N>) super.or(expr);
        }

        @Override
        public Num<N> or(N value) {
            return (Num<N>) super.or(value);
        }

        @Override
        public NumericExpression<N> build() {
            return (NumericExpression<N>) super.build();
        }
    }

    public static class Cmp<T extends Comparable<?>> extends CoalesceBuilder<T> {

        Cmp(Expression<T> expression) {
            super(expression);
        }

        @Override
        public Cmp<T> or(Expression<T> expr) {
            return (Cmp<T>) super.or(expr);
        }

        @Override
        public Cmp<T> or(T value) {
            return (Cmp<T>) super.or(value);
        }

        @Override
        public ComparableExpression<T> build() {
            return (ComparableExpression<T>) super.build();
        }
    }

    private static class Expr<T> extends AbstractExpression<T> {

        @Override
        public Class<T> getType() {
            return ((ExpressionImplementor<T>)expressions.get(0)).getType();
        }

        @Override
        public int precedence() {
            return 0;
        }

        private List<Expression<?>> expressions;

        public Expr(List<Expression<?>> expressions) {
            this.expressions = expressions;
        }

        @Override
        public void accept(@NotNull AstVisitor visitor) {
            for (Expression<?> expression : expressions) {
                ((Ast) expression).accept(visitor);
            }
        }

        @Override
        public void renderTo(@NotNull SqlBuilder builder) {
            if  (expressions.size() == 1) {
                renderChild((Ast) expressions.get(0), builder);
            } else {
                builder.sql("coalesce(");
                String separator = "";
                for (Expression<?> expression : expressions) {
                    builder.sql(separator);
                    renderChild((Ast) expression, builder);
                    separator = ", ";
                }
                builder.sql(")");
            }
        }
    }

    private static class StrExpr extends Expr<String> implements StringExpressionImplementor {

        public StrExpr(List<Expression<?>> expressions) {
            super(expressions);
        }
    }

    private static class NumExpr<N extends Number & Comparable<N>> extends Expr<N> implements NumericExpressionImplementor<N> {

        public NumExpr(List<Expression<?>> expressions) {
            super(expressions);
        }
    }

    private static class CmpExpr<T extends Comparable<?>> extends Expr<T> implements ComparableExpressionImplementor<T> {

        public CmpExpr(List<Expression<?>> expressions) {
            super(expressions);
        }
    }
}

