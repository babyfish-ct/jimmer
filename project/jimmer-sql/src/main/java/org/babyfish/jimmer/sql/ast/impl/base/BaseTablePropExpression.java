package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ast.PropExpression;
import org.babyfish.jimmer.sql.ast.impl.*;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.table.RealTable;
import org.babyfish.jimmer.sql.ast.impl.table.TableProxies;
import org.babyfish.jimmer.sql.meta.ColumnDefinition;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.spi.PropExpressionImplementor;
import org.babyfish.jimmer.sql.meta.EmbeddedColumns;
import org.babyfish.jimmer.sql.meta.FormulaTemplate;
import org.babyfish.jimmer.sql.meta.MetadataStrategy;
import org.babyfish.jimmer.sql.meta.SqlTemplate;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.temporal.Temporal;
import java.util.Date;

class BaseTablePropExpression<T> implements PropExpressionImplementor<T>, Ast {

    private final PropExpressionImplementor<T> raw;

    private final BaseTableOwner baseTableOwner;

    BaseTablePropExpression(PropExpressionImplementor<T> raw, BaseTableOwner baseTableOwner) {
        if (raw instanceof BaseTablePropExpression<?>) {
            raw = ((BaseTablePropExpression<T>) raw).raw;
        }
        this.raw = raw;
        this.baseTableOwner = baseTableOwner;
    }

    BaseTableOwner getBaseTableOwner() {
        return baseTableOwner;
    }

    @Override
    public Table<?> getTable() {
        return raw.getTable();
    }

    @Override
    public ImmutableProp getProp() {
        return raw.getProp();
    }

    @Override
    public ImmutableProp getDeepestProp() {
        return raw.getDeepestProp();
    }

    @Override
    public PropExpressionImpl.@Nullable EmbeddedImpl<?> getBase() {
        return raw.getBase();
    }

    @Override
    public @Nullable String getPath() {
        return raw.getPath();
    }

    @Override
    public boolean isRawId() {
        return raw.isRawId();
    }

    @Override
    public EmbeddedColumns.@Nullable Partial getPartial(MetadataStrategy strategy) {
        return raw.getPartial(strategy);
    }

    @Override
    public void renderTo(@NotNull AbstractSqlBuilder<?> builder, boolean ignoreBrackets) {
        renderTo(builder, ignoreBrackets, false);
    }

    @Override
    public PropExpression<T> unwrap() {
        return this;
    }

    @Override
    public Class<T> getType() {
        return raw.getType();
    }

    @Override
    public int precedence() {
        return raw.precedence();
    }

    @Override
    public void accept(@NotNull AstVisitor visitor) {
        AstContext ctx = visitor.getAstContext();
        visitor.visitBaseTableExpression(baseTableOwner);
        ctx.pushStatement(baseTableOwner.getBaseTable().getQuery().getMutableQuery());
        try {
            ((Ast) raw.unwrap()).accept(visitor);
        } finally {
            ctx.popStatement();
        }
    }

    @Override
    public void renderTo(@NotNull AbstractSqlBuilder<?> builder) {
        renderTo(builder, false, true);
    }

    private void renderTo(@NotNull AbstractSqlBuilder<?> builder, boolean ignoreBrackets, boolean simpleCall) {
        AstContext ctx = builder.assertSimple().getAstContext();
        ctx.pushStatement(baseTableOwner.getBaseTable().getQuery().getMutableQuery());
        try {
            BaseSelectionMapper mapper = ctx.getBaseSelectionMapper(baseTableOwner);
            assert mapper != null;
            RealTable realTable = TableProxies.resolve(raw.getTable(), ctx).realTable(ctx);
            if (mapper.isRootTable(realTable)) {
                renderExportedProp(builder, mapper, realTable, ignoreBrackets);
                return;
            }
        } finally {
            ctx.popStatement();
        }
        if (simpleCall) {
            ((Ast) raw.unwrap()).renderTo(builder);
        } else {
            raw.renderTo(builder, ignoreBrackets);
        }
    }

    private void renderExportedProp(
            AbstractSqlBuilder<?> builder,
            BaseSelectionMapper mapper,
            RealTable realTable,
            boolean ignoreBrackets
    ) {
        SqlTemplate template = raw.getProp().getSqlTemplate();
        if (template instanceof FormulaTemplate) {
            renderExportedColumn(builder, mapper.formulaIndex(realTable.getAlias(), (FormulaTemplate) template));
            return;
        }
        if (!raw.getProp().isColumnDefinition()) {
            builder.sql(mapper.getAlias()).sql(".c").sql(Integer.toString(mapper.expressionIndex()));
            return;
        }
        ColumnDefinition definition = raw.getProp().getStorage(
                builder.sqlClient().getMetadataStrategy()
        );
        if (!ignoreBrackets && definition.size() > 1) {
            builder.enter(SqlBuilder.ScopeType.TUPLE);
            renderExportedColumns(builder, mapper, realTable, definition);
            builder.leave();
        } else {
            renderExportedColumns(builder, mapper, realTable, definition);
        }
    }

    private void renderExportedColumns(
            AbstractSqlBuilder<?> builder,
            BaseSelectionMapper mapper,
            RealTable realTable,
            ColumnDefinition definition
    ) {
        String alias = realTable.getFinalAlias(raw.getProp(), raw.isRawId(), builder.sqlClient());
        int size = definition.size();
        for (int i = 0; i < size; i++) {
            if (i != 0) {
                builder.sql(", ");
            }
            renderExportedColumn(
                    builder,
                    mapper.columnIndex(alias, definition.name(i), false)
            );
        }
    }

    private void renderExportedColumn(AbstractSqlBuilder<?> builder, int index) {
        builder.sql(mapperAlias(builder)).sql(".c").sql(Integer.toString(index));
    }

    private String mapperAlias(AbstractSqlBuilder<?> builder) {
        AstContext ctx = builder.assertSimple().getAstContext();
        BaseSelectionMapper mapper = ctx.getBaseSelectionMapper(baseTableOwner);
        assert mapper != null;
        return mapper.getAlias();
    }

    @Override
    public boolean hasVirtualPredicate() {
        return ((Ast) raw.unwrap()).hasVirtualPredicate();
    }

    @Override
    public Ast resolveVirtualPredicate(AstContext ctx) {
        return ((Ast) raw.unwrap()).resolveVirtualPredicate(ctx);
    }

    static class Cmp<T extends Comparable<?>>
            extends BaseTablePropExpression<T>
            implements PropExpression.Cmp<T> {

        Cmp(PropExpressionImplementor<T> raw, BaseTableOwner baseTableOwner) {
            super(raw, baseTableOwner);
        }
    }

    static class Str extends Cmp<String> implements PropExpression.Str {

        Str(PropExpressionImplementor<String> raw, BaseTableOwner baseTableOwner) {
            super(raw, baseTableOwner);
        }
    }

    static class Num<N extends Number & Comparable<N>>
            extends Cmp<N>
            implements PropExpression.Num<N> {

        Num(PropExpressionImplementor<N> raw, BaseTableOwner baseTableOwner) {
            super(raw, baseTableOwner);
        }
    }

    static class Dt<T extends Date & Comparable<Date>>
        extends Cmp<T>
        implements PropExpression.Dt<T> {

        Dt(PropExpressionImplementor<T> raw, BaseTableOwner baseTableOwner) {
            super(raw, baseTableOwner);
        }
    }

    static class Tp<T extends Temporal & Comparable<?>>
        extends Cmp<T>
        implements PropExpression.Tp<T> {

        Tp(PropExpressionImplementor<T> raw, BaseTableOwner baseTableOwner) {
            super(raw, baseTableOwner);
        }
    }
}
