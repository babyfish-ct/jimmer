package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.Predicate;

public interface PredicateImplementor extends Predicate, ExpressionImplementor<Boolean> {

    Predicate[] EMPTY_PREDICATES = new Predicate[0];

    @Override
    default Predicate and(Predicate other) {
        return CompositePredicate.and(this, other);
    }

    @Override
    default Predicate or(Predicate other) {
        return CompositePredicate.or(this, other);
    }

    @Override
    default Predicate not() {
        return new NotPredicate(this);
    }
}
