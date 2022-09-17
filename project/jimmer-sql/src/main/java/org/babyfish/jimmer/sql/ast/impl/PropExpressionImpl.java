package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.sql.ast.*;
import org.babyfish.jimmer.sql.meta.Column;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.NotNull;

public class PropExpressionImpl<T>
        extends AbstractExpression<T>
        implements PropExpression<T> {

    private TableImplementor<?> table;

    private ImmutableProp prop;

    public static PropExpressionImpl<?> of(TableImplementor<?> table, ImmutableProp prop) {
        Class<?> elementClass = prop.getElementClass();
        if (String.class.isAssignableFrom(elementClass)) {
            return new StrImpl(table, prop);
        }
        if (elementClass.isPrimitive() || Number.class.isAssignableFrom(elementClass)) {
            return new NumImpl<>(table, prop);
        }
        if (Comparable.class.isAssignableFrom(elementClass)) {
            return new CmpImpl<>(table, prop);
        }
        return new PropExpressionImpl<>(table, prop);
    }

    PropExpressionImpl(TableImplementor<?> table, ImmutableProp prop) {
        if (prop.isAssociation(TargetLevel.ENTITY)) {
            throw new IllegalArgumentException("prop '" + prop + "' cannot be association property");
        }
        if (!(prop.getStorage() instanceof Column)) {
            throw new IllegalArgumentException("prop is not selectable");
        }
        this.table = table;
        this.prop = prop;
    }

    public TableImplementor<?> getTableImplementor() {
        return table;
    }

    public ImmutableProp getProp() {
        return prop;
    }

    @Override
    public void accept(@NotNull AstVisitor visitor) {
        visitor.visitTableReference(table, prop);
    }

    @Override
    public void renderTo(@NotNull SqlBuilder builder) {
        table.renderSelection(prop, builder);
    }

    @Override
    public int precedence() {
        return 0;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<T> getType() {
        return (Class<T>) prop.getElementClass();
    }

    private static class StrImpl
            extends PropExpressionImpl<String>
            implements PropExpression.Str, StringExpressionImplementor {

        StrImpl(TableImplementor<?> table, ImmutableProp prop) {
            super(table, prop);
        }

        @Override
        public StringExpression coalesce(String defaultValue) {
            return StringExpressionImplementor.super.coalesce(defaultValue);
        }

        @Override
        public StringExpression coalesce(Expression<String> defaultExpr) {
            return StringExpressionImplementor.super.coalesce(defaultExpr);
        }

        @Override
        public CoalesceBuilder.Str coalesceBuilder() {
            return StringExpressionImplementor.super.coalesceBuilder();
        }

        @Override
        public TableImplementor<?> getTableImplementor() {
            return super.getTableImplementor();
        }
    }

    private static class NumImpl<N extends Number>
            extends PropExpressionImpl<N>
            implements PropExpression.Num<N>, NumericExpressionImplementor<N> {

        NumImpl(TableImplementor<?> table, ImmutableProp prop) {
            super(table, prop);
        }

        @Override
        public NumericExpression<N> coalesce(N defaultValue) {
            return NumericExpressionImplementor.super.coalesce(defaultValue);
        }

        @Override
        public NumericExpression<N> coalesce(Expression<N> defaultExpr) {
            return NumericExpressionImplementor.super.coalesce(defaultExpr);
        }

        @Override
        public CoalesceBuilder.Num<N> coalesceBuilder() {
            return NumericExpressionImplementor.super.coalesceBuilder();
        }
    }

    private static class CmpImpl<T extends Comparable<T>>
            extends PropExpressionImpl<T>
            implements PropExpression.Cmp<T>, ComparableExpressionImplementor<T> {

        CmpImpl(TableImplementor<?> table, ImmutableProp prop) {
            super(table, prop);
        }

        @Override
        public ComparableExpression<T> coalesce(T defaultValue) {
            return ComparableExpressionImplementor.super.coalesce(defaultValue);
        }

        @Override
        public ComparableExpression<T> coalesce(Expression<T> defaultExpr) {
            return ComparableExpressionImplementor.super.coalesce(defaultExpr);
        }

        @Override
        public CoalesceBuilder.Cmp<T> coalesceBuilder() {
            return ComparableExpressionImplementor.super.coalesceBuilder();
        }
    }
}
