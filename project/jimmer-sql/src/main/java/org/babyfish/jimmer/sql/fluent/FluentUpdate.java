package org.babyfish.jimmer.sql.fluent;

import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.sql.ast.Executable;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.PropExpression;

import java.util.function.Supplier;

public interface FluentUpdate extends FluentFilterable, Executable<Integer> {

    @OldChain
    @Override
    FluentUpdate where(Predicate... predicates);

    @OldChain
    @Override
    default FluentUpdate whereIf(boolean condition, Supplier<Predicate> predicateSupplier) {
        return (FluentUpdate) FluentFilterable.super.whereIf(condition, predicateSupplier);
    }

    @OldChain
    <X> FluentUpdate set(PropExpression<X> path, X value);

    @OldChain
    <X> FluentUpdate set(PropExpression<X> path, Expression<X> value);
}
