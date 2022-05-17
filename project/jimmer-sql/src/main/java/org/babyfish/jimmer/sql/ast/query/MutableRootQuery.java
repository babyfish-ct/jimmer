package org.babyfish.jimmer.sql.ast.query;

import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.query.selectable.RootSelectable;
import org.babyfish.jimmer.sql.ast.table.Table;

public interface MutableRootQuery<T extends Table<?>> extends MutableQuery, RootSelectable<T> {

    @OldChain
    @Override
    MutableRootQuery<T> where(Predicate... predicates);

    @OldChain
    @SuppressWarnings("unchecked")
    @Override
    default MutableRootQuery<T> orderBy(Expression<?> expression) {
        return (MutableRootQuery<T>) MutableQuery.super.orderBy(expression);
    }

    @OldChain
    @SuppressWarnings("unchecked")
    @Override
    default MutableRootQuery<T> orderBy(Expression<?> expression, OrderMode orderMode) {
        return (MutableRootQuery<T>) MutableQuery.super.orderBy(expression, orderMode);
    }

    @OldChain
    @Override
    MutableRootQuery<T> orderBy(Expression<?> expression, OrderMode orderMode, NullOrderMode nullOrderMode);

    @OldChain
    @Override
    MutableRootQuery<T> groupBy(Expression<?>... expressions);

    @OldChain
    @Override
    MutableRootQuery<T> having(Predicate... predicates);
}
