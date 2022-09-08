package org.babyfish.jimmer.sql.ast.query;

import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.query.selectable.SubSelectable;

public interface MutableSubQuery extends MutableQuery, SubSelectable {

    @OldChain
    @Override
    MutableSubQuery where(Predicate... predicates);

    @OldChain
    @Override
    default MutableSubQuery orderBy(Expression<?> ... expressions) {
        return (MutableSubQuery) MutableQuery.super.orderBy(expressions);
    }

    @OldChain
    @Override
    MutableSubQuery orderBy(Order ... orders);

    @OldChain
    @Override
    MutableSubQuery groupBy(Expression<?>... expressions);

    @OldChain
    @Override
    MutableSubQuery having(Predicate... predicates);

    Predicate exists();

    Predicate notExists();
}
