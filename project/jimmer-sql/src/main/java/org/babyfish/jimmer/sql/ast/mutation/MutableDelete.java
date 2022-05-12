package org.babyfish.jimmer.sql.ast.mutation;

import org.babyfish.jimmer.sql.ast.Predicate;

public interface MutableDelete {

    MutableDelete where(Predicate... predicates);
}
