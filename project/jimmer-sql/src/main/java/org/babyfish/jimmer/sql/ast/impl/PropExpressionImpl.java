package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.EmbeddableDto;
import org.babyfish.jimmer.meta.EmbeddedLevel;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.sql.ast.*;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.render.BatchSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.table.FetcherSelectionImpl;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.TableProxies;
import org.babyfish.jimmer.sql.ast.impl.value.ValueGetter;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.spi.PropExpressionImplementor;
import org.babyfish.jimmer.sql.fetcher.DtoMetadata;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.meta.EmbeddedColumns;
import org.babyfish.jimmer.sql.meta.FormulaTemplate;
import org.babyfish.jimmer.sql.meta.MetadataStrategy;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.temporal.Temporal;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class PropExpressionImpl<T>
        extends AbstractExpression<T>
        implements PropExpressionImplementor<T> {

    protected final Table<?> table;

    protected final ImmutableProp prop;

    protected final ImmutableProp deepestProp;

    protected final EmbeddedImpl<?> base;

    protected final String path;

    protected final boolean rawId;

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
        if (Date.class.isAssignableFrom(elementClass)) {
            return new DtImpl<>(base, prop);
        }
        if (Temporal.class.isAssignableFrom(elementClass)) {
            return new TpImpl<>(base, prop);
        }
        if (Comparable.class.isAssignableFrom(elementClass)) {
            return new CmpImpl<>(base, prop);
        }
        return new PropExpressionImpl<>(base, prop);
    }

    public static PropExpressionImpl<?> of(Table<?> table, ImmutableProp prop, boolean rawId) {
        if (prop.isTransient()) {
            throw new IllegalArgumentException(
                    "Cannot create prop expression for transient property \"" +
                            prop +
                            "\""
            );
        }
        if (prop.isView()) {
            throw new IllegalArgumentException(
                    "Cannot create prop expression for view property \"" +
                            prop +
                            "\""
            );
        }
        if (!prop.getDependencies().isEmpty()) {
            throw new IllegalArgumentException(
                    "Cannot create prop expression for java/kotlin based calculated property \"" +
                            prop +
                            "\""
            );
        }
        if (prop.isAssociation(TargetLevel.PERSISTENT)) {
            throw new IllegalArgumentException(
                    "Cannot create prop expression for java/kotlin based association property \"" +
                            prop +
                            "\""
            );
        }
        if (prop.isEmbedded(EmbeddedLevel.SCALAR)) {
            return new EmbeddedImpl<>(table, prop, rawId);
        }
        Class<?> elementClass = prop.getElementClass();
        if (String.class.isAssignableFrom(elementClass)) {
            return new StrImpl(table, prop, rawId);
        }
        if (elementClass.isPrimitive() || Number.class.isAssignableFrom(elementClass)) {
            return new NumImpl<>(table, prop, rawId);
        }
        if (Date.class.isAssignableFrom(elementClass)) {
            return new DtImpl<>(table, prop, rawId);
        }
        if (Temporal.class.isAssignableFrom(elementClass)) {
            return new TpImpl<>(table, prop, rawId);
        }
        if (Comparable.class.isAssignableFrom(elementClass)) {
            return new CmpImpl<>(table, prop, rawId);
        }
        return new PropExpressionImpl<>(table, prop, rawId);
    }

    PropExpressionImpl(Table<?> table, ImmutableProp prop, boolean rawId) {
        if (prop.isAssociation(TargetLevel.PERSISTENT)) {
            throw new IllegalArgumentException("The property '" + prop + "' cannot be association property");
        }
        if (!prop.isColumnDefinition() && !(prop.getSqlTemplate() instanceof FormulaTemplate)) {
            throw new IllegalArgumentException("The property '" + prop + "' is not selectable");
        }
        this.table = table;
        this.prop = prop;
        this.deepestProp = prop;
        this.base = null;
        this.path = null;
        this.rawId = rawId && prop.isId();
    }

    PropExpressionImpl(EmbeddedImpl<?> base, ImmutableProp prop) {
        if (prop.isAssociation(TargetLevel.PERSISTENT)) {
            throw new IllegalArgumentException("prop '" + prop + "' cannot be association property");
        }
        this.table = base.table;
        this.prop = base.getProp();
        this.deepestProp = prop;
        this.base = base;
        this.path = base.path == null ? prop.getName() : base.path + "." + prop.getName();
        this.rawId = base.rawId;
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
    public ImmutableProp getDeepestProp() {
        return deepestProp;
    }

    @Override
    public EmbeddedImpl<?> getBase() {
        return base;
    }

    @Nullable
    @Override
    public String getPath() {
        return path;
    }

    @Override
    public boolean isRawId() {
        return rawId;
    }

    @Nullable
    @Override
    public EmbeddedColumns.Partial getPartial(MetadataStrategy strategy) {
        if (base != null || prop.isEmbedded(EmbeddedLevel.SCALAR)) {
            return prop.<EmbeddedColumns>getStorage(strategy).partial(path);
        }
        return null;
    }

    @Override
    public void accept(@NotNull AstVisitor visitor) {
        visitor.visitTableReference(
                TableProxies
                        .resolve(table, visitor.getAstContext())
                        .realTable(visitor.getAstContext()),
                prop,
                rawId
        );
    }

    @Override
    protected boolean determineHasVirtualPredicate() {
        return false;
    }

    @Override
    protected Ast onResolveVirtualPredicate(AstContext ctx) {
        return this;
    }

    @Override
    public void renderTo(@NotNull AbstractSqlBuilder<?> builder) {
        if (builder instanceof BatchSqlBuilder) {
            renderTo((BatchSqlBuilder) builder);
        } else {
            renderTo(builder, false);
        }
    }

    @Override
    public void renderTo(@NotNull AbstractSqlBuilder<?> abstractBuilder, boolean ignoreBrackets) {
        SqlBuilder builder = abstractBuilder.assertSimple();
        TableImplementor<?> tableImplementor = TableProxies.resolve(table, builder.getAstContext());
        EmbeddedColumns.Partial partial = getPartial(builder.getAstContext().getSqlClient().getMetadataStrategy());
        if (partial != null) {
            if (ignoreBrackets || partial.size() == 1) {
                tableImplementor.renderSelection(prop, rawId, builder, path != null ? partial : null);
            } else {
                builder.enter(SqlBuilder.ScopeType.TUPLE);
                tableImplementor.renderSelection(prop, rawId, builder, path != null ? partial : null);
                builder.leave();
            }
        } else {
            tableImplementor.renderSelection(prop, rawId, builder, null);
        }
    }

    private void renderTo(@NotNull BatchSqlBuilder builder) {
        List<ValueGetter> getters = ValueGetter.valueGetters(builder.sqlClient(), this, null);
        if (getters.size() != 1) {
            throw new IllegalStateException(
                    "The type fo \"" +
                            PropExpression.class.getName() +
                            "\" rendered by \"" +
                            BatchSqlBuilder.class.getName() +
                            "\" cannot be tuple or embeddable type"
            );
        }
        ValueGetter getter = getters.get(0);
        builder.sql(getter);
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PropExpressionImpl<?> that = (PropExpressionImpl<?>) o;
        return table.equals(that.table) && prop.equals(that.prop) && Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(table, prop, path);
    }

    @Override
    public String toString() {
        if (path == null) {
            return table.toString() + '.' + prop.getName();
        }
        return table.toString() + '.' + prop.getName() + '.' + path;
    }

    @Override
    public PropExpressionImpl<T> unwrap() {
        return this;
    }

    private static class StrImpl
            extends PropExpressionImpl<String>
            implements PropExpression.Str {

        StrImpl(Table<?> table, ImmutableProp prop, boolean rawId) {
            super(table, prop, rawId);
        }

        StrImpl(EmbeddedImpl<?> base, ImmutableProp prop) {
            super(base, prop);
        }

        @Override
        public Table<?> getTable() {
            return super.getTable();
        }
    }

    private static class NumImpl<N extends Number & Comparable<N>>
            extends PropExpressionImpl<N>
            implements PropExpression.Num<N> {

        NumImpl(Table<?> table, ImmutableProp prop, boolean rawId) {
            super(table, prop, rawId);
        }

        NumImpl(EmbeddedImpl<?> base, ImmutableProp prop) {
            super(base, prop);
        }
    }

    private static class DtImpl<T extends Date & Comparable<Date>>
            extends PropExpressionImpl<T>
            implements PropExpression.Dt<T> {

        DtImpl(Table<?> table, ImmutableProp prop, boolean rawId) {
            super(table, prop, rawId);
        }

        DtImpl(EmbeddedImpl<?> base, ImmutableProp prop) {
            super(base, prop);
        }
    }

    private static class TpImpl<T extends Temporal & Comparable<?>>
            extends PropExpressionImpl<T>
            implements PropExpression.Tp<T> {

        TpImpl(Table<?> table, ImmutableProp prop, boolean rawId) {
            super(table, prop, rawId);
        }

        TpImpl(EmbeddedImpl<?> base, ImmutableProp prop) {
            super(base, prop);
        }
    }

    private static class CmpImpl<T extends Comparable<?>>
            extends PropExpressionImpl<T>
            implements PropExpression.Cmp<T> {

        CmpImpl(Table<?> table, ImmutableProp prop, boolean rawId) {
            super(table, prop, rawId);
        }

        CmpImpl(EmbeddedImpl<?> base, ImmutableProp prop) {
            super(base, prop);
        }
    }

    public static class EmbeddedImpl<T>
            extends PropExpressionImpl<T>
            implements PropExpression.Embedded<T> {

        protected EmbeddedImpl(Table<?> table, ImmutableProp prop, boolean rawId) {
            super(table, prop, rawId);
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

        @SuppressWarnings("unchecked")
        @Override
        public <XE extends Expression<?>> XE get(ImmutableProp prop) {
            if (prop.getDeclaringType() != this.deepestProp.getTargetType()) {
                throw new IllegalArgumentException(
                        "The property \"" +
                                prop +
                                "\" does not belong to the current embeddable type \"" +
                                this.deepestProp.getTargetType() +
                                "\""
                );
            }
            return (XE)PropExpressionImpl.of(this, prop);
        }

        @Override
        public Selection<T> fetch(Fetcher<T> fetcher) {
            return new FetcherSelectionImpl<>(this, fetcher);
        }

        @Override
        public <V extends EmbeddableDto<T>> Selection<V> fetch(Class<V> valueType) {
            if (valueType == null) {
                throw new IllegalArgumentException("The argument `valueType` cannot be null");
            }
            DtoMetadata<T, V> metadata = DtoMetadata.of(valueType);
            Fetcher<?> fetcher = metadata.getFetcher();
            if (this.deepestProp.getTargetType() != fetcher.getImmutableType()) {
                throw new IllegalArgumentException(
                        "Illegal fetcher type, the embeddable type of current prop expression is \"" +
                                this.deepestProp.getTargetType() +
                                "\" but the static type is based on \"" +
                                fetcher.getImmutableType() +
                                "\""
                );
            }
            return new FetcherSelectionImpl<V>(this, fetcher, metadata.getConverter());
        }

        @Override
        public final @NotNull Expression<T> coalesce(T defaultValue) {
            return Embedded.super.coalesce(defaultValue);
        }

        @Override
        public final @NotNull Expression<T> coalesce(Expression<T> defaultExpr) {
            return Embedded.super.coalesce(defaultExpr);
        }

        @Override
        public final @NotNull CoalesceBuilder<T> coalesceBuilder() {
            return Embedded.super.coalesceBuilder();
        }
    }
}
