package org.babyfish.jimmer.sql.ast.mutation;

import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.sql.ast.Executable;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.query.Filterable;

import java.util.function.Supplier;

public interface MutableDelete extends Filterable, Executable<Integer> {

    @OldChain
    MutableDelete where(Predicate... predicates);

    @OldChain
    @Override
    default MutableDelete whereIf(boolean condition, Predicate predicate) {
        if (condition) {
            where(predicate);
        }
        return this;
    }

    @OldChain
    @Override
    default MutableDelete whereIf(boolean condition, Supplier<Predicate> block) {
        if (condition) {
            where(block.get());
        }
        return this;
    }

    MutableDelete disableDissociation();

    MutableDelete setMode(DeleteMode mode);
}
