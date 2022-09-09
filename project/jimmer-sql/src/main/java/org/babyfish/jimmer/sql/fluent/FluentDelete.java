package org.babyfish.jimmer.sql.fluent;

import org.babyfish.jimmer.sql.ast.Executable;
import org.babyfish.jimmer.sql.ast.Predicate;

public interface FluentDelete extends FluentFilterable, Executable<Integer> {

    @Override
    FluentDelete where(Predicate... predicates);

    @Override
    default FluentDelete whereIf(boolean condition, Predicate... predicates) {
        return (FluentDelete) FluentFilterable.super.whereIf(condition, predicates);
    }
}
