package org.babyfish.jimmer.sql.ast.mutation;

import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.sql.ast.Executable;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.PropExpression;
import org.babyfish.jimmer.sql.ast.query.Filterable;

import java.util.function.Supplier;

public interface MutableUpdate extends Filterable, Executable<Integer> {

    @OldChain
    <X> MutableUpdate set(PropExpression<X> path, X value);

    @OldChain
    <X> MutableUpdate set(PropExpression<X> path, Expression<X> value);

    @OldChain
    @Override
    MutableUpdate where(Predicate ... predicates);

    @OldChain
    @Deprecated
    @Override
    default MutableUpdate whereIf(boolean condition, Predicate predicate) {
        if (condition) {
            where(predicate);
        }
        return this;
    }

    @OldChain
    @Override
    default MutableUpdate whereIf(boolean condition, Supplier<Predicate> block) {
        if (condition) {
            where(block.get());
        }
        return this;
    }
}
