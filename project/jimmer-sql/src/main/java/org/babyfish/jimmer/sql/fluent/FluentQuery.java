package org.babyfish.jimmer.sql.fluent;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.query.Order;

public interface FluentQuery extends FluentFilterable {

    @Override
    FluentQuery where(Predicate... predicates);

    @Override
    default FluentQuery whereIf(boolean condition, Predicate... predicates) {
        return (FluentQuery) FluentFilterable.super.whereIf(condition, predicates);
    }

    FluentQuery groupBy(Expression<?>... expressions);

    FluentQuery having(Predicate... predicates);

    default FluentQuery havingIf(boolean condition, Predicate... predicates) {
        if (condition) {
            having(predicates);
        }
        return this;
    }

    FluentQuery orderBy(Expression<?>... expressions);

    default FluentQuery orderByIf(boolean condition, Expression<?>... expressions) {
        if (condition) {
            orderBy(expressions);
        }
        return this;
    }

    FluentQuery orderBy(Order... orders);

    default FluentQuery orderByIf(boolean condition, Order... orders) {
        if (condition) {
            orderBy(orders);
        }
        return this;
    }
}
