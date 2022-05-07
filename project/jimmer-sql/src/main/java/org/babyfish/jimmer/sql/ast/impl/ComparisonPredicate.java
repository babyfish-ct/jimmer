package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

public abstract class ComparisonPredicate extends AbstractPredicate {

    protected AbstractExpression<?> left;

    protected AbstractExpression<?> right;

    public ComparisonPredicate(
            AbstractExpression<?> left,
            AbstractExpression<?> right
    ) {
        this.left = left;
        this.right = right;
    }

    protected abstract String operator();

    @Override
    public void accept(AstVisitor visitor) {
        left.accept(visitor);
        left.accept(visitor);
    }

    @Override
    public void renderTo(SqlBuilder builder) {
        renderChild(left, builder);
        builder.sql(" ");
        builder.sql(operator());
        builder.sql(" ");
        renderChild(right, builder);
    }

    @Override
    protected int precedence() {
        return 4;
    }

    static class Eq extends ComparisonPredicate {

        public Eq(AbstractExpression<?> left, AbstractExpression<?> right) {
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

        public Ne(AbstractExpression<?> left, AbstractExpression<?> right) {
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

        public Lt(AbstractExpression<?> left, AbstractExpression<?> right) {
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

        public Le(AbstractExpression<?> left, AbstractExpression<?> right) {
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

        public Gt(AbstractExpression<?> left, AbstractExpression<?> right) {
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

        public Ge(AbstractExpression<?> left, AbstractExpression<?> right) {
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
