package org.babyfish.jimmer.sql.ast.query;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.query.selectable.RootSelectable;

public interface MutableRootQuery extends MutableQuery, RootSelectable {

    @Override
    MutableRootQuery where(Predicate... predicates);

    @Override
    default MutableRootQuery orderBy(Expression<?> expression) {
        return (MutableRootQuery) MutableQuery.super.orderBy(expression);
    }

    @Override
    default MutableRootQuery orderBy(Expression<?> expression, OrderMode orderMode) {
        return (MutableRootQuery) MutableQuery.super.orderBy(expression, orderMode);
    }

    @Override
    MutableRootQuery orderBy(Expression<?> expression, OrderMode orderMode, NullOrderMode nullOrderMode);

    @Override
    MutableRootQuery groupBy(Expression<?>... expressions);

    @Override
    MutableRootQuery having(Predicate... predicates);
}
