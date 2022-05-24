package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.Predicate;

interface PredicateImplementor extends Predicate {

    @Override
    default Predicate and(Predicate other) {
        return other != null ?
                new CompositePredicate.And(this, (AbstractPredicate)other) :
                this;
    }

    @Override
    default Predicate or(Predicate other) {
        return other != null ?
                new CompositePredicate.Or(this, (AbstractPredicate)other) :
                this;
    }

    @Override
    default Predicate not() {
        return new NotPredicate(this);
    }
}
