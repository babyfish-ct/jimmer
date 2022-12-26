package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.NotNull;

abstract class BinaryExpression<N extends Number & Comparable<N>> extends AbstractExpression<N> implements NumericExpressionImplementor<N> {

    private Class<N> type;
    
    private Expression<N> left;

    private Expression<N> right;

    protected BinaryExpression(Class<N> type, Expression<N> left, Expression<N> right) {
        this.type = type;
        this.left = left;
        this.right = right;
    }

    @Override
    public Class<N> getType() {
        return type;
    }

    protected abstract String operator();

    @Override
    public void accept(@NotNull AstVisitor visitor) {
        ((Ast) left).accept(visitor);
        ((Ast) right).accept(visitor);
    }

    @Override
    public void renderTo(@NotNull SqlBuilder builder) {
        renderChild((Ast) left, builder);
        builder.sql(" ");
        builder.sql(operator());
        builder.sql(" ");
        renderChild((Ast) right, builder);
    }
    
    static class Plus<N extends Number & Comparable<N>> extends BinaryExpression<N> {
        
        public Plus(Class<N> type, Expression<N> left, Expression<N> right) {
            super(type, left, right);
        }

        @Override
        public int precedence() {
            return ExpressionPrecedences.PLUS;
        }

        @Override
        protected String operator() {
            return "+";
        }
    }

    static class Minus<N extends Number & Comparable<N>> extends BinaryExpression<N> {

        public Minus(Class<N> type, Expression<N> left, Expression<N> right) {
            super(type, left, right);
        }

        @Override
        public int precedence() {
            return ExpressionPrecedences.PLUS;
        }

        @Override
        protected String operator() {
            return "-";
        }
    }

    static class Times<N extends Number & Comparable<N>> extends BinaryExpression<N> {

        public Times(Class<N> type, Expression<N> left, Expression<N> right) {
            super(type, left, right);
        }

        @Override
        public int precedence() {
            return ExpressionPrecedences.TIMES;
        }

        @Override
        protected String operator() {
            return "*";
        }
    }

    static class Div<N extends Number & Comparable<N>> extends BinaryExpression<N> {

        public Div(Class<N> type, Expression<N> left, Expression<N> right) {
            super(type, left, right);
        }

        @Override
        public int precedence() {
            return ExpressionPrecedences.TIMES;
        }

        @Override
        protected String operator() {
            return "/";
        }
    }

    static class Rem<N extends Number & Comparable<N>> extends BinaryExpression<N> {

        public Rem(Class<N> type, Expression<N> left, Expression<N> right) {
            super(type, left, right);
        }

        @Override
        public int precedence() {
            return ExpressionPrecedences.TIMES;
        }

        @Override
        protected String operator() {
            return "%";
        }
    }
}
