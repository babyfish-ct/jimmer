package org.babyfish.jimmer.sql.fluent;

import org.babyfish.jimmer.sql.ast.Executable;
import org.babyfish.jimmer.sql.ast.Predicate;

import java.util.function.Supplier;

public interface FluentDelete extends FluentFilterable, Executable<Integer> {

    @Override
    FluentDelete where(Predicate... predicates);

    @Override
    default FluentDelete whereIf(boolean condition, Supplier<Predicate> predicateSupplier) {
        return (FluentDelete) FluentFilterable.super.whereIf(condition, predicateSupplier);
    }
}
