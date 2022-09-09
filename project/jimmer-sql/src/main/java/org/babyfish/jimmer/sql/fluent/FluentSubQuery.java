package org.babyfish.jimmer.sql.fluent;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.query.Order;
import org.babyfish.jimmer.sql.ast.query.selectable.SubSelectable;

public interface FluentSubQuery extends FluentQuery, SubSelectable {

    @Override
    FluentSubQuery where(Predicate... predicates);

    @Override
    default FluentSubQuery whereIf(boolean condition, Predicate... predicates) {
        return (FluentSubQuery) FluentQuery.super.whereIf(condition, predicates);
    }

    FluentSubQuery groupBy(Expression<?>... expressions);

    FluentSubQuery having(Predicate... predicates);

    @Override
    default FluentSubQuery havingIf(boolean condition, Predicate... predicates) {
        return (FluentSubQuery) FluentQuery.super.havingIf(condition, predicates);
    }

    FluentSubQuery orderBy(Expression<?>... expressions);

    @Override
    default FluentSubQuery orderByIf(boolean condition, Expression<?>... expressions) {
        return (FluentSubQuery) FluentQuery.super.orderByIf(condition, expressions);
    }

    FluentSubQuery orderBy(Order... orders);

    @Override
    default FluentSubQuery orderByIf(boolean condition, Order... orders) {
        return (FluentSubQuery) FluentQuery.super.orderByIf(condition, orders);
    }

    Predicate exists();

    Predicate notExists();
}
