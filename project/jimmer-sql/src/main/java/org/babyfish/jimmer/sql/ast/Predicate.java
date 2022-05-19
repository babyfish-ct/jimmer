package org.babyfish.jimmer.sql.ast;

import org.babyfish.jimmer.lang.NewChain;

public interface Predicate extends Expression<Boolean> {

    @NewChain
    Predicate and(Predicate other);

    @NewChain
    Predicate or(Predicate other);

    @NewChain
    Predicate not();
}
