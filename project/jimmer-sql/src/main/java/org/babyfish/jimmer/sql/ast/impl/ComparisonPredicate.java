package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.NotNull;

abstract class ComparisonPredicate extends AbstractPredicate {

    protected Expression<?> left;

    protected Expression<?> right;

    public ComparisonPredicate(
            Expression<?> left,
            Expression<?> right
    ) {
        this.left = left;
        this.right = right;
        Literals.bind(left, right);
        Literals.bind(right, left);
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

    @Override
    public int precedence() {
        return ExpressionPrecedences.COMPARISON;
    }

    static class Eq extends ComparisonPredicate {

        public Eq(Expression<?> left, Expression<?> right) {
            super(left, right);
        }

        @Override
        protected String operator() {
            return "=";
        }

        @Override
        public Predicate not() {
            return new Ne(left, right);
        }
    }

    static class Ne extends ComparisonPredicate {

        public Ne(Expression<?> left, Expression<?> right) {
            super(left, right);
        }

        @Override
        protected String operator() {
            return "<>";
        }

        @Override
        public Predicate not() {
            return new Eq(left, right);
        }
    }

    static class Lt extends ComparisonPredicate {

        public Lt(Expression<?> left, Expression<?> right) {
            super(left, right);
        }

        @Override
        protected String operator() {
            return "<";
        }

        @Override
        public Predicate not() {
            return new Ge(left, right);
        }
    }

    static class Le extends ComparisonPredicate {

        public Le(Expression<?> left, Expression<?> right) {
            super(left, right);
        }

        @Override
        protected String operator() {
            return "<=";
        }

        @Override
        public Predicate not() {
            return new Gt(left, right);
        }
    }

    static class Gt extends ComparisonPredicate {

        public Gt(Expression<?> left, Expression<?> right) {
            super(left, right);
        }

        @Override
        protected String operator() {
            return ">";
        }

        @Override
        public Predicate not() {
            return new Le(left, right);
        }
    }

    static class Ge extends ComparisonPredicate {

        public Ge(Expression<?> left, Expression<?> right) {
            super(left, right);
        }

        @Override
        protected String operator() {
            return ">=";
        }

        @Override
        public Predicate not() {
            return new Lt(left, right);
        }
    }
}
