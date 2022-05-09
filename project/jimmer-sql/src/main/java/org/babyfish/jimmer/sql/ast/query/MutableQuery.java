package org.babyfish.jimmer.sql.ast.query;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.table.Table;

public interface MutableQuery extends Filterable {

    MutableQuery groupBy(Expression<?> ... expressions);

    MutableQuery having(Predicate ... predicates);
}
