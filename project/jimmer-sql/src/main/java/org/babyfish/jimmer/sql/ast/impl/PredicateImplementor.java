package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.Predicate;

public interface PredicateImplementor extends Predicate, ExpressionImplementor<Boolean> {

    @Override
    default Predicate and(Predicate other) {
        return other != null ?
                new CompositePredicate.And(this, other) :
                this;
    }

    @Override
    default Predicate or(Predicate other) {
        return other != null ?
                new CompositePredicate.Or(this, other) :
                this;
    }

    @Override
    default Predicate not() {
        return new NotPredicate(this);
    }
}
