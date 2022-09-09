package org.babyfish.jimmer.sql.fluent;

import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.sql.ast.Predicate;

public interface FluentFilterable {

    @OldChain
    FluentFilterable where(Predicate... predicates);

    @OldChain
    default FluentFilterable whereIf(boolean condition, Predicate... predicates) {
        if (condition) {
            where(predicates);
        }
        return this;
    }
}
