package org.babyfish.jimmer.sql.fluent;

import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.sql.ast.Executable;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.PropExpression;

public interface FluentUpdate extends FluentFilterable, Executable<Integer> {

    @OldChain
    @Override
    FluentUpdate where(Predicate... predicates);

    @OldChain
    <X> FluentUpdate set(PropExpression<X> path, X value);

    @OldChain
    <X> FluentUpdate set(PropExpression<X> path, Expression<X> value);
}
