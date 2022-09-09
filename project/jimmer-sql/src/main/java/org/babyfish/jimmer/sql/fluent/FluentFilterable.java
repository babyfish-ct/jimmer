package org.babyfish.jimmer.sql.fluent;

import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.sql.ast.Predicate;

import java.util.function.Supplier;

public interface FluentFilterable {

    @OldChain
    FluentFilterable where(Predicate... predicates);

    @OldChain
    default FluentFilterable whereIf(boolean condition, Supplier<Predicate> predicateSupplier) {
        if (condition) {
            where(predicateSupplier.get());
        }
        return this;
    }
}
