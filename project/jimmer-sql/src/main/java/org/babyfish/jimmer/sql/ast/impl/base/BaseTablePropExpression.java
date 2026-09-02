package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ast.PropExpression;
import org.babyfish.jimmer.sql.ast.impl.Ast;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.PropExpressionImpl;
import org.babyfish.jimmer.sql.ast.impl.query.QueryRenderContext;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.table.RealTable;
import org.babyfish.jimmer.sql.ast.impl.table.TableProxies;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.spi.PropExpressionImplementor;
import org.babyfish.jimmer.sql.meta.EmbeddedColumns;
import org.babyfish.jimmer.sql.meta.MetadataStrategy;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.temporal.Temporal;
import java.util.Date;

class BaseTablePropExpression<T>
        extends AbstractBaseTableExpression<T, PropExpressionImplementor<T>>
        implements PropExpressionImplementor<T> {

    BaseTablePropExpression(PropExpressionImplementor<T> raw, BaseTableOwner baseTableOwner) {
        super(unwrap(raw), baseTableOwner);
    }

    private static <T> PropExpressionImplementor<T> unwrap(PropExpressionImplementor<T> raw) {
        if (raw instanceof BaseTablePropExpression<?>) {
            return ((BaseTablePropExpression<T>) raw).raw();
        }
        return raw;
    }

    @Override
    protected Ast rawAst() {
        return (Ast) raw().unwrap();
    }

    @Override
    public Table<?> getTable() {
        return raw().getTable();
    }

    @Override
    public ImmutableProp getProp() {
        return raw().getProp();
    }

    @Override
    public ImmutableProp getDeepestProp() {
        return raw().getDeepestProp();
    }

    @Override
    public PropExpressionImpl.@Nullable EmbeddedImpl<?> getBase() {
        return raw().getBase();
    }

    @Override
    public @Nullable String getPath() {
        return raw().getPath();
    }

    @Override
    public boolean isRawId() {
        return raw().isRawId();
    }

    @Override
    public EmbeddedColumns.@Nullable Partial getPartial(MetadataStrategy strategy) {
        return raw().getPartial(strategy);
    }

    @Override
    public void renderTo(@NotNull AbstractSqlBuilder<?> builder, boolean ignoreBrackets) {
        renderWithMutationReplacement(
                builder,
                () -> renderWithoutReplacement(builder, ignoreBrackets, false)
        );
    }

    @Override
    public PropExpression<T> unwrap() {
        return this;
    }

    @Override
    protected void renderWithoutReplacement(AbstractSqlBuilder<?> builder) {
        renderWithoutReplacement(builder, false, true);
    }

    private void renderWithoutReplacement(
            AbstractSqlBuilder<?> builder,
            boolean ignoreBrackets,
            boolean simpleCall
    ) {
        AstContext ctx = builder.assertSimple().getAstContext();
        QueryRenderContext renderContext = builder.assertSimple().getQueryRenderContext();
        ctx.pushStatement(getBaseTableOwner().getBaseTable().getQuery().getMutableQuery());
        try {
            RealTable realTable = TableProxies.resolve(raw().getTable(), ctx).realTable(renderContext);
            BaseQueryReadSupport readSupport = renderContext.getBaseQueryReadSupport();
            BaseQueryRead read = readSupport.propExpression(
                    getBaseTableOwner(),
                    raw(),
                    realTable,
                    builder.sqlClient().getMetadataStrategy()
            );
            if (read != null) {
                renderExportedRead(builder, read, ignoreBrackets);
                return;
            }
            readSupport.requireSelection(getBaseTableOwner());
        } finally {
            ctx.popStatement();
        }
        if (simpleCall) {
            rawAst().renderTo(builder);
        } else {
            raw().renderTo(builder, ignoreBrackets);
        }
    }

    private void renderExportedRead(AbstractSqlBuilder<?> builder, BaseQueryRead read, boolean ignoreBrackets) {
        if (!ignoreBrackets && read.size() > 1) {
            builder.enter(SqlBuilder.ScopeType.TUPLE);
            renderExportedColumns(builder, read);
            builder.leave();
        } else {
            renderExportedColumns(builder, read);
        }
    }

    private void renderExportedColumns(AbstractSqlBuilder<?> builder, BaseQueryRead read) {
        int size = read.size();
        for (int i = 0; i < size; i++) {
            if (i != 0) {
                builder.sql(", ");
            }
            renderExportedColumn(builder, read, i);
        }
    }

    private void renderExportedColumn(AbstractSqlBuilder<?> builder, BaseQueryRead read, int index) {
        builder
                .sql(builder.assertSimple().alias(read.getRealBaseTable()))
                .sql(".c")
                .sql(Integer.toString(read.index(index)));
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
