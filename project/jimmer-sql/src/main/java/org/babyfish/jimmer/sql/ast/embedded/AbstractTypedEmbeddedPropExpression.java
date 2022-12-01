package org.babyfish.jimmer.sql.ast.embedded;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.NumericExpression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.PropExpression;
import org.babyfish.jimmer.sql.ast.impl.CoalesceBuilder;
import org.babyfish.jimmer.sql.ast.query.Order;
import org.babyfish.jimmer.sql.ast.query.TypedSubQuery;

import java.util.Collection;

public abstract class AbstractTypedEmbeddedPropExpression<T> {

    private final PropExpression.Embedded<T> raw;

    protected AbstractTypedEmbeddedPropExpression(PropExpression.Embedded<T> raw) {
        this.raw = raw;
    }

    public Predicate eq(Expression<T> other) {
        return raw.eq(other);
    }

    public Predicate eq(T other) {
        return raw.eq(other);
    }

    public Predicate ne(Expression<T> other) {
        return raw.ne(other);
    }

    public Predicate ne(T other) {
        return raw.ne(other);
    }

    public Predicate isNull() {
        return raw.isNull();
    }

    public Predicate isNotNull() {
        return raw.isNotNull();
    }

    public Predicate in(Collection<T> values) {
        return raw.in(values);
    }

    public Predicate notIn(Collection<T> values) {
        return raw.notIn(values);
    }

    public Predicate in(TypedSubQuery<T> subQuery) {
        return raw.in(subQuery);
    }

    public Predicate notIn(TypedSubQuery<T> subQuery) {
        return raw.notIn(subQuery);
    }

    public NumericExpression<Long> count() {
        return raw.count();
    }

    public NumericExpression<Long> count(boolean distinct) {
        return raw.count(distinct);
    }

    public Order asc() {
        return raw.asc();
    }

    public Order desc() {
        return raw.desc();
    }

    public <XE extends Expression<?>> XE get(String prop) {
        return raw.get(prop);
    }

    public Expression<T> coalesce(T defaultValue) {
        return raw.coalesce(defaultValue);
    }

    public Expression<T> coalesce(Expression<T> defaultExpr) {
        return raw.coalesce(defaultExpr);
    }

    public CoalesceBuilder<T> coalesceBuilder() {
        return raw.coalesceBuilder();
    }
}
