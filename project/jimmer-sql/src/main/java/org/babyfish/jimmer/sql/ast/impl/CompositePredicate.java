package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

abstract class CompositePredicate extends AbstractPredicate {

    private Predicate[] predicates;

    public CompositePredicate(
            Predicate[] predicates
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
            renderChild((Ast) predicates[i], builder);
        }
    }

    protected abstract String operator();

    static class And extends CompositePredicate {

        public And(Predicate ... predicates) {
            super(predicates);
        }

        @Override
        protected String operator() {
            return "and";
        }

        @Override
        public int precedence() {
            return 6;
        }
    }

    static class Or extends CompositePredicate {

        public Or(Predicate ... predicates) {
            super(predicates);
        }

        @Override
        protected String operator() {
            return "or";
        }

        @Override
        public int precedence() {
            return 7;
        }
    }
}
