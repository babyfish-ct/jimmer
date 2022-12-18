package org.babyfish.jimmer.sql.ast.query;

import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.query.selectable.RootSelectable;
import org.babyfish.jimmer.sql.ast.table.Table;

import java.util.function.Supplier;

public interface MutableRootQuery<T extends Table<?>> extends MutableQuery, RootSelectable<T> {

    @OldChain
    @Override
    MutableRootQuery<T> where(Predicate... predicates);

    @OldChain
    @Override
    default MutableRootQuery<T> whereIf(boolean condition, Predicate predicates) {
        if (condition) {
            where(predicates);
        }
        return this;
    }

    @OldChain
    @Override
    default MutableRootQuery<T> whereIf(boolean condition, Supplier<Predicate> block) {
        if (condition) {
            where(block.get());
        }
        return this;
    }

    @OldChain
    @SuppressWarnings("unchecked")
    @Override
    default MutableRootQuery<T> orderBy(Expression<?>... expressions) {
        return (MutableRootQuery<T>) MutableQuery.super.orderBy(expressions);
    }

    @OldChain
    @SuppressWarnings("unchecked")
    @Override
    default MutableRootQuery<T> orderByIf(boolean condition, Expression<?>... expressions) {
        return (MutableRootQuery<T>) MutableQuery.super.orderByIf(condition, expressions);
    }

    @OldChain
    @Override
    MutableRootQuery<T> orderBy(Order... orders);

    @OldChain
    @SuppressWarnings("unchecked")
    @Override
    default MutableRootQuery<T> orderByIf(boolean condition, Order... orders) {
        return (MutableRootQuery<T>) MutableQuery.super.orderByIf(condition, orders);
    }

    @Override
    MutableRootQuery<T> groupBy(Expression<?>... expressions);

    @Override
    MutableRootQuery<T> having(Predicate... predicates);
}
