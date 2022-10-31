package org.babyfish.jimmer.sql.ast.mutation;

import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.sql.ast.Executable;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.PropExpression;
import org.babyfish.jimmer.sql.ast.query.Filterable;

public interface MutableUpdate extends Filterable, Executable<Integer> {

    @OldChain
    <X> MutableUpdate set(PropExpression<X> path, X value);

    @OldChain
    <X> MutableUpdate set(PropExpression<X> path, Expression<X> value);

    @OldChain
    MutableUpdate where(Predicate ... predicates);
}
