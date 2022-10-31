package org.babyfish.jimmer.sql.ast.mutation;

import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.sql.ast.Executable;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.query.Filterable;

public interface MutableDelete extends Filterable, Executable<Integer> {

    @OldChain
    MutableDelete where(Predicate... predicates);
}
