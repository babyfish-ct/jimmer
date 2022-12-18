package org.babyfish.jimmer.sql.ast.query;

import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.table.Table;

public interface MutableQuery extends Sortable {

    @OldChain
    @Override
    default MutableQuery orderBy(Expression<?>... expressions) {
        return (MutableQuery) Sortable.super.orderBy(expressions);
    }

    @OldChain
    @Override
    MutableQuery orderBy(Order... orders);

    @OldChain
    @Override
    default MutableQuery orderByIf(boolean condition, Expression<?>... expressions) {
        return (MutableQuery) Sortable.super.orderByIf(condition, expressions);
    }

    @OldChain
    @Override
    default MutableQuery orderByIf(boolean condition, Order... orders) {
        return (MutableQuery) Sortable.super.orderByIf(condition, orders);
    }

    @OldChain
    MutableQuery groupBy(Expression<?> ... expressions);

    @OldChain
    MutableQuery having(Predicate ... predicates);
}
