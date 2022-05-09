package org.babyfish.jimmer.sql.ast.query;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.query.selectable.RootSelectable;
import org.babyfish.jimmer.sql.ast.table.Table;

public interface MutableRootQuery<T extends Table<?>> extends MutableQuery, RootSelectable<T> {

    @Override
    MutableRootQuery<T> where(Predicate... predicates);

    @SuppressWarnings("unchecked")
    @Override
    default MutableRootQuery<T> orderBy(Expression<?> expression) {
        return (MutableRootQuery<T>) MutableQuery.super.orderBy(expression);
    }

    @SuppressWarnings("unchecked")
    @Override
    default MutableRootQuery<T> orderBy(Expression<?> expression, OrderMode orderMode) {
        return (MutableRootQuery<T>) MutableQuery.super.orderBy(expression, orderMode);
    }

    @Override
    MutableRootQuery<T> orderBy(Expression<?> expression, OrderMode orderMode, NullOrderMode nullOrderMode);

    @Override
    MutableRootQuery<T> groupBy(Expression<?>... expressions);

    @Override
    MutableRootQuery<T> having(Predicate... predicates);
}
