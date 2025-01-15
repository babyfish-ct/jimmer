package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.Predicate;

public interface PredicateImplementor extends Predicate, ExpressionImplementor<Boolean> {

    Predicate[] EMPTY_PREDICATES = new Predicate[0];

    default Predicate not() {
        return new NotPredicate(this);
    }
}
