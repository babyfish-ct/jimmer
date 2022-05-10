package org.babyfish.jimmer.sql.ast.mutation;

import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.mutation.MutableUpdate;

public interface MutableDelete {

    MutableUpdate where(Predicate... predicates);
}
