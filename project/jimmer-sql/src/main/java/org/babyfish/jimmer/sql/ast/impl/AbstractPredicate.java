package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

abstract class AbstractPredicate extends AbstractExpression<Boolean> implements Predicate {

    @Override
    public Predicate and(Predicate other) {
        return other != null ?
                new AndPredicate(this, (AbstractPredicate)other) :
                this;
    }

    @Override
    public Predicate or(Predicate other) {
        return other != null ?
                new OrPredicate(this, (AbstractPredicate)other) :
                this;
    }

    @Override
    public Predicate not() {
        return new NotPredicate(this);
    }

    @Override
    public Class<Boolean> getType() {
        return Boolean.class;
    }

    private static abstract class CompositePredicate extends AbstractPredicate {

        private AbstractPredicate[] predicates;

        public CompositePredicate(
                AbstractPredicate[] predicates
        ) {
            this.predicates = predicates;
        }

        @Override
        public void accept(AstVisitor visitor) {
            for (Predicate predicate : predicates) {
                ((Ast) predicate).accept(visitor);
            }
        }

        @Override
        public void renderTo(SqlBuilder builder) {
            String sp = ' ' + operator() + ' ';
            int size = predicates.length;
            for (int i = 0; i < size; i++) {
                if (i != 0) {
                    builder.sql(sp);
                }
                renderChild(predicates[i], builder);
            }
        }

        protected abstract String operator();
    }

    private static class AndPredicate extends CompositePredicate {

        public AndPredicate(AbstractPredicate ... predicates) {
            super(predicates);
        }

        @Override
        protected String operator() {
            return "and";
        }

        @Override
        protected int precedence() {
            return 6;
        }
    }

    private static class OrPredicate extends CompositePredicate {

        public OrPredicate(AbstractPredicate ... predicates) {
            super(predicates);
        }

        @Override
        protected String operator() {
            return "or";
        }

        @Override
        protected int precedence() {
            return 7;
        }
    }

    private static class NotPredicate extends AbstractPredicate {

        private AbstractPredicate predicate;

        public NotPredicate(AbstractPredicate predicate) {
            this.predicate = predicate;
        }

        @Override
        public void accept(AstVisitor visitor) {
            ((Ast)predicate).accept(visitor);
        }

        @Override
        public void renderTo(SqlBuilder builder) {
            builder.sql("not ");
            renderChild(predicate, builder);
        }

        @Override
        protected int precedence() {
            return 5;
        }
    }
}
