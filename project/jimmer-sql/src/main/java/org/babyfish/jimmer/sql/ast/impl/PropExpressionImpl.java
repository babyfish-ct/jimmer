package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.meta.EmbeddedLevel;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.sql.ast.*;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.TableProxies;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.spi.PropExpressionImplementor;
import org.babyfish.jimmer.sql.meta.ColumnDefinition;
import org.babyfish.jimmer.sql.meta.EmbeddedColumns;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.NotNull;

public class PropExpressionImpl<T>
        extends AbstractExpression<T>
        implements PropExpressionImplementor<T> {

    protected final Table<?> table;

    protected final ImmutableProp prop;

    protected final ImmutableProp deepestProp;

    protected final EmbeddedImpl<?> base;

    protected final String path;

    public static PropExpressionImpl<?> of(EmbeddedImpl<?> base, ImmutableProp prop) {
        if (prop.isEmbedded(EmbeddedLevel.SCALAR)) {
            return new EmbeddedImpl<>(base, prop);
        }
        Class<?> elementClass = prop.getElementClass();
        if (String.class.isAssignableFrom(elementClass)) {
            return new StrImpl(base, prop);
        }
        if (elementClass.isPrimitive() || Number.class.isAssignableFrom(elementClass)) {
            return new NumImpl<>(base, prop);
        }
        if (Comparable.class.isAssignableFrom(elementClass)) {
            return new CmpImpl<>(base, prop);
        }
        return new PropExpressionImpl<>(base, prop);
    }

    public static PropExpressionImpl<?> of(Table<?> table, ImmutableProp prop) {
        if (prop.isEmbedded(EmbeddedLevel.SCALAR)) {
            return new EmbeddedImpl<>(table, prop);
        }
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

    PropExpressionImpl(Table<?> table, ImmutableProp prop) {
        if (prop.isAssociation(TargetLevel.ENTITY)) {
            throw new IllegalArgumentException("prop '" + prop + "' cannot be association property");
        }
        if (!(prop.getStorage() instanceof ColumnDefinition)) {
            throw new IllegalArgumentException("prop is not selectable");
        }
        this.table = table;
        this.prop = prop;
        this.deepestProp = prop;
        this.base = null;
        this.path = null;
    }

    PropExpressionImpl(EmbeddedImpl<?> base, ImmutableProp prop) {
        if (prop.isAssociation(TargetLevel.ENTITY)) {
            throw new IllegalArgumentException("prop '" + prop + "' cannot be association property");
        }
        this.table = base.table;
        this.prop = base.getProp();
        this.deepestProp = prop;
        this.base = base;
        this.path = base.path == null ? prop.getName() : base.path + "." + prop.getName();
    }

    @Override
    public Table<?> getTable() {
        return table;
    }

    @Override
    public ImmutableProp getProp() {
        return prop;
    }

    @Override
    public EmbeddedColumns.Partial getPartial() {
        if (base != null || prop.isEmbedded(EmbeddedLevel.SCALAR)) {
            return ((EmbeddedColumns)prop.getStorage()).partial(path);
        }
        return null;
    }

    @Override
    public void accept(@NotNull AstVisitor visitor) {
        visitor.visitTableReference(TableProxies.resolve(table, visitor.getAstContext()), prop);
    }

    @Override
    public void renderTo(@NotNull SqlBuilder builder) {
        renderTo(builder, false);
    }

    @Override
    public void renderTo(@NotNull SqlBuilder builder, boolean ignoreEmbeddedTuple) {
        TableImplementor<?> tableImplementor = TableProxies.resolve(table, builder.getAstContext());
        EmbeddedColumns.Partial partial = getPartial();
        if (partial != null) {
            if (ignoreEmbeddedTuple || partial.size() == 1) {
                tableImplementor.renderSelection(prop, builder, path != null ? partial : null);
            } else {
                builder.enterTuple();
                tableImplementor.renderSelection(prop, builder, path != null ? partial : null);
                builder.leaveTuple();
            }
        } else {
            tableImplementor.renderSelection(prop, builder, null);
        }
    }

    @Override
    public int precedence() {
        return 0;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<T> getType() {
        return (Class<T>) deepestProp.getElementClass();
    }

    @Override
    public String toString() {
        if (path == null) {
            return prop.toString();
        }
        return prop.toString() + '.' + path;
    }

    private static class StrImpl
            extends PropExpressionImpl<String>
            implements PropExpression.Str, StringExpressionImplementor {

        StrImpl(Table<?> table, ImmutableProp prop) {
            super(table, prop);
        }

        StrImpl(EmbeddedImpl<?> base, ImmutableProp prop) {
            super(base, prop);
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
        public Table<?> getTable() {
            return super.getTable();
        }
    }

    private static class NumImpl<N extends Number>
            extends PropExpressionImpl<N>
            implements PropExpression.Num<N>, NumericExpressionImplementor<N> {

        NumImpl(Table<?> table, ImmutableProp prop) {
            super(table, prop);
        }

        NumImpl(EmbeddedImpl<?> base, ImmutableProp prop) {
            super(base, prop);
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

    private static class CmpImpl<T extends Comparable<?>>
            extends PropExpressionImpl<T>
            implements PropExpression.Cmp<T>, ComparableExpressionImplementor<T> {

        CmpImpl(Table<?> table, ImmutableProp prop) {
            super(table, prop);
        }

        CmpImpl(EmbeddedImpl<?> base, ImmutableProp prop) {
            super(base, prop);
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

    public static class EmbeddedImpl<T>
            extends PropExpressionImpl<T>
            implements PropExpression.Embedded<T> {

        protected EmbeddedImpl(Table<?> table, ImmutableProp prop) {
            super(table, prop);
        }

        protected EmbeddedImpl(EmbeddedImpl<?> base, ImmutableProp prop) {
            super(base, prop);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <XE extends Expression<?>> XE get(String prop) {
            ImmutableProp deeperProp = this.deepestProp.getTargetType().getProp(prop);
            return (XE)PropExpressionImpl.of(this, deeperProp);
        }

        @Override
        public final Expression<T> coalesce(T defaultValue) {
            return Embedded.super.coalesce(defaultValue);
        }

        @Override
        public final Expression<T> coalesce(Expression<T> defaultExpr) {
            return Embedded.super.coalesce(defaultExpr);
        }

        @Override
        public final CoalesceBuilder<T> coalesceBuilder() {
            return Embedded.super.coalesceBuilder();
        }
    }
}
