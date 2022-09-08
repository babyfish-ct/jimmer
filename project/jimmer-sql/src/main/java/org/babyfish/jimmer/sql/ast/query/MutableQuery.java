package org.babyfish.jimmer.sql.ast.query;

import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.table.Table;

public interface MutableQuery extends Sortable {

    @OldChain
    MutableQuery groupBy(Expression<?> ... expressions);

    @OldChain
    MutableQuery having(Predicate ... predicates);
}
