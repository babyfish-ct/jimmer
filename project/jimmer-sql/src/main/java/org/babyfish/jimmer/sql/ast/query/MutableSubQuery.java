package org.babyfish.jimmer.sql.ast.query;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.query.selectable.SubSelectable;

public interface MutableSubQuery extends MutableQuery, SubSelectable {

    @Override
    MutableSubQuery where(Predicate... predicates);

    @Override
    default MutableSubQuery orderBy(Expression<?> expression) {
        return (MutableSubQuery) MutableQuery.super.orderBy(expression);
    }

    @Override
    default MutableSubQuery orderBy(Expression<?> expression, OrderMode orderMode) {
        return (MutableSubQuery) MutableQuery.super.orderBy(expression, orderMode);
    }

    @Override
    MutableSubQuery orderBy(Expression<?> expression, OrderMode orderMode, NullOrderMode nullOrderMode);

    @Override
    MutableSubQuery groupBy(Expression<?>... expressions);

    @Override
    MutableSubQuery having(Predicate... predicates);
}
