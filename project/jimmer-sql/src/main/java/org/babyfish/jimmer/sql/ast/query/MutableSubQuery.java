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
    default MutableSubQuery orderBy(Expression<?> expression) {
        return (MutableSubQuery) MutableQuery.super.orderBy(expression);
    }

    @SuppressWarnings("unchecked")
    @OldChain
    @Override
    default MutableSubQuery orderBy(Expression<?> expression, OrderMode orderMode) {
        return (MutableSubQuery) MutableQuery.super.orderBy(expression, orderMode);
    }

    @OldChain
    @Override
    MutableSubQuery orderBy(Expression<?> expression, OrderMode orderMode, NullOrderMode nullOrderMode);

    @OldChain
    @Override
    MutableSubQuery groupBy(Expression<?>... expressions);

    @OldChain
    @Override
    MutableSubQuery having(Predicate... predicates);

    Predicate exists();

    Predicate notExists();
}
