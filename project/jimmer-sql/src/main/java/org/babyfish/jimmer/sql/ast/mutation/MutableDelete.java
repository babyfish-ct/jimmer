package org.babyfish.jimmer.sql.ast.mutation;

import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.query.Filterable;

public interface MutableDelete extends Filterable {

    MutableDelete where(Predicate... predicates);
}
