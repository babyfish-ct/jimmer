package org.babyfish.jimmer.sql.fluent;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.query.Order;
import org.babyfish.jimmer.sql.ast.query.selectable.RootSelectable;
import org.babyfish.jimmer.sql.ast.table.Table;

public interface FluentRootQuery<T extends Table<?>> extends FluentQuery, RootSelectable<T> {

    @Override
    FluentRootQuery<T> where(Predicate... predicates);

    @SuppressWarnings("unchecked")
    @Override
    default FluentRootQuery<T> whereIf(boolean condition, Predicate... predicates) {
        return (FluentRootQuery<T>) FluentQuery.super.whereIf(condition, predicates);
    }

    FluentRootQuery<T> groupBy(Expression<?>... expressions);

    FluentRootQuery<T> having(Predicate... predicates);

    @SuppressWarnings("unchecked")
    @Override
    default FluentRootQuery<T> havingIf(boolean condition, Predicate... predicates) {
        return (FluentRootQuery<T>) FluentQuery.super.havingIf(condition, predicates);
    }

    @SuppressWarnings("unchecked")
    FluentRootQuery<T> orderBy(Expression<?>... expressions);

    @Override
    default FluentRootQuery<T> orderByIf(boolean condition, Expression<?>... expressions) {
        return (FluentRootQuery<T>) FluentQuery.super.orderByIf(condition, expressions);
    }

    FluentRootQuery<T> orderBy(Order... orders);

    @SuppressWarnings("unchecked")
    @Override
    default FluentRootQuery<T> orderByIf(boolean condition, Order... orders) {
        return (FluentRootQuery<T>) FluentQuery.super.orderByIf(condition, orders);
    }
}
