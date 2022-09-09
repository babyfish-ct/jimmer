package org.babyfish.jimmer.sql.fluent;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.query.Order;

import java.util.function.Supplier;

public interface FluentQuery extends FluentFilterable {

    @Override
    FluentQuery where(Predicate... predicates);

    @Override
    default FluentQuery whereIf(boolean condition, Supplier<Predicate> predicateSupplier) {
        return (FluentQuery) FluentFilterable.super.whereIf(condition, predicateSupplier);
    }

    FluentQuery groupBy(Expression<?>... expressions);

    FluentQuery having(Predicate... predicates);

    default FluentQuery havingIf(boolean condition, Supplier<Predicate> predicateSupplier) {
        if (condition) {
            having(predicateSupplier.get());
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
