package org.babyfish.jimmer.sql.ast.embedded;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ast.*;
import org.babyfish.jimmer.sql.ast.impl.CoalesceBuilder;
import org.babyfish.jimmer.sql.ast.query.Order;
import org.babyfish.jimmer.sql.ast.query.TypedSubQuery;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.spi.PropExpressionImplementor;
import org.babyfish.jimmer.sql.meta.EmbeddedColumns;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public abstract class AbstractTypedEmbeddedPropExpression<T> implements PropExpressionImplementor<T> {

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

    @Override
    public Table<?> getTable() {
        return ((PropExpressionImplementor<?>)raw).getTable();
    }

    @Override
    public ImmutableProp getProp() {
        return ((PropExpressionImplementor<?>)raw).getProp();
    }

    @Override
    public EmbeddedColumns.Partial getPartial() {
        return ((PropExpressionImplementor<?>)raw).getPartial();
    }

    @Override
    public void renderTo(@NotNull SqlBuilder builder, boolean ignoreEmbeddedTuple) {
        ((PropExpressionImplementor<?>)raw).renderTo(builder, ignoreEmbeddedTuple);
    }

    @Override
    public String toString() {
        return raw.toString();
    }

    @SuppressWarnings("unchecked")
    public static <T> T unwrap(Selection<?> selection) {
        if (selection instanceof AbstractTypedEmbeddedPropExpression<?>) {
            return (T)((AbstractTypedEmbeddedPropExpression<?>)selection).raw;
        }
        return (T)selection;
    }
}
