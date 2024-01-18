package org.babyfish.jimmer.sql.ast.embedded;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ast.*;
import org.babyfish.jimmer.sql.ast.impl.CoalesceBuilder;
import org.babyfish.jimmer.sql.ast.impl.PropExpressionImpl;
import org.babyfish.jimmer.sql.ast.query.Order;
import org.babyfish.jimmer.sql.ast.query.TypedSubQuery;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.spi.PropExpressionImplementor;
import org.babyfish.jimmer.sql.meta.EmbeddedColumns;
import org.babyfish.jimmer.sql.meta.MetadataStrategy;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public abstract class AbstractTypedEmbeddedPropExpression<T> implements PropExpressionImplementor<T>, PropExpression.Embedded<T> {

    private final PropExpression.Embedded<T> raw;

    protected AbstractTypedEmbeddedPropExpression(PropExpression.Embedded<T> raw) {
        if (raw instanceof AbstractTypedEmbeddedPropExpression<?>) {
            throw new IllegalArgumentException("raw cannot be " + AbstractTypedEmbeddedPropExpression.class.getName());
        }
        this.raw = raw;
    }

    @Override
    public Class<T> getType() {
        return ((PropExpressionImpl.EmbeddedImpl<T>)raw).getType();
    }

    @Override
    public int precedence() {
        return ((PropExpressionImpl.EmbeddedImpl<T>)raw).precedence();
    }

    public @NotNull Predicate eq(@NotNull Expression<T> other) {
        return raw.eq(other);
    }

    public @NotNull Predicate eq(T other) {
        return raw.eq(other);
    }

    public @NotNull Predicate ne(@NotNull Expression<T> other) {
        return raw.ne(other);
    }

    public @NotNull Predicate ne(T other) {
        return raw.ne(other);
    }

    public @NotNull Predicate isNull() {
        return raw.isNull();
    }

    public @NotNull Predicate isNotNull() {
        return raw.isNotNull();
    }

    public @NotNull Predicate in(@NotNull Collection<T> values) {
        return raw.in(values);
    }

    public @NotNull Predicate notIn(@NotNull Collection<T> values) {
        return raw.notIn(values);
    }

    public @NotNull Predicate in(@NotNull TypedSubQuery<T> subQuery) {
        return raw.in(subQuery);
    }

    public @NotNull Predicate notIn(@NotNull TypedSubQuery<T> subQuery) {
        return raw.notIn(subQuery);
    }

    public @NotNull NumericExpression<Long> count() {
        return raw.count();
    }

    public @NotNull NumericExpression<Long> count(boolean distinct) {
        return raw.count(distinct);
    }

    public @NotNull Order asc() {
        return raw.asc();
    }

    public @NotNull Order desc() {
        return raw.desc();
    }

    public <XE extends Expression<?>> XE get(String prop) {
        return raw.get(prop);
    }

    public <XE extends Expression<?>> XE get(ImmutableProp prop) {
        return raw.get(prop);
    }

    public @NotNull Expression<T> coalesce(T defaultValue) {
        return raw.coalesce(defaultValue);
    }

    public @NotNull Expression<T> coalesce(Expression<T> defaultExpr) {
        return raw.coalesce(defaultExpr);
    }

    public @NotNull CoalesceBuilder<T> coalesceBuilder() {
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
    public ImmutableProp getDeepestProp() {
        return ((PropExpressionImplementor<?>)raw).getDeepestProp();
    }

    @Override
    public PropExpressionImpl.EmbeddedImpl<?> getBase() {
        return ((PropExpressionImplementor<?>)raw).getBase();
    }

    @Override
    public boolean isRawId() {
        return ((PropExpressionImplementor<?>)raw).isRawId();
    }

    @Override
    public EmbeddedColumns.Partial getPartial(MetadataStrategy strategy) {
        return ((PropExpressionImplementor<?>)raw).getPartial(strategy);
    }

    @Override
    public void renderTo(@NotNull SqlBuilder builder, boolean ignoreBrackets) {
        ((PropExpressionImplementor<?>)raw).renderTo(builder, ignoreBrackets);
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

    public <EXP extends PropExpression<?>> EXP __get(ImmutableProp prop) {
        return raw.get(prop);
    }
}
