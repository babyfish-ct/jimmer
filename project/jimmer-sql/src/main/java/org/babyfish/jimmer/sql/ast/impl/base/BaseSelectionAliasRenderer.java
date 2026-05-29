package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.Ast;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.query.ConfigurableBaseQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.query.QueryRenderContext;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.table.RealTable;
import org.babyfish.jimmer.sql.ast.impl.table.TableProxies;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

import java.util.List;
import java.util.Map;

final class BaseSelectionAliasRenderer implements BaseSelectionAliasRender {

    private final Map<RealTable, BaseQueryExport> exportMap;

    private final boolean cte;

    BaseSelectionAliasRenderer(Map<RealTable, BaseQueryExport> exportMap, BaseTableSymbol baseTableSymbol) {
        this.exportMap = exportMap;
        this.cte = baseTableSymbol.isCte();
    }

    @Override
    public void render(int index, Selection<?> selection, SqlBuilder builder) {
        RealTable realBaseTable = builder.getAstContext().getRenderedRealBaseTable();
        BaseQueryExport export = exportMap.get(realBaseTable);
        BaseQueryExportSelection exportSelection = export != null ?
                export.selectionOrNull(index, rootRealTable(selection, builder)) :
                null;
        if (exportSelection == null) {
            return;
        }

        if (selection instanceof Expression<?>) {
            builder.separator();
            ((Ast) selection).renderTo(builder);
            if (!cte) {
                builder.sql(" c").sql(Integer.toString(exportSelection.expressionIndex()));
            }
            return;
        }
        RealTable realTable = TableProxies.resolve((Table<?>) selection, builder.getAstContext())
                .realTable(builder.getQueryRenderContext());
        for (BaseQueryExportColumn column : exportSelection.columns()) {
            RealTable childTable = childTableByKeys(realTable, column.getTableKeys());
            if (column.isForeignKeyInBaseQuery()) {
                RealTable newChildTable = childTable.getParent();
                if (newChildTable != null) {
                    childTable = newChildTable;
                }
            }
            String alias = childTable.getAlias();
            builder.separator();
            if (column.getFormula() != null) {
                builder.sql(column.getFormula().toSql(alias));
            } else {
                builder
                        .sql(alias)
                        .sql(".")
                        .sql(column.getName());
            }
            if (!cte) {
                builder.sql(" c").sql(Integer.toString(column.getIndex()));
            }
        }
    }

    @Override
    public void renderCteColumns(RealTable realBaseTable, SqlBuilder builder) {
        BaseTableImplementor baseTableImplementor = (BaseTableImplementor) realBaseTable.getTableLikeImplementor();
        ConfigurableBaseQueryImpl<?> query = baseTableImplementor.toSymbol().getQuery();
        List<Selection<?>> selections = query.getSelections();
        int size = selections.size();
        builder.enter(AbstractSqlBuilder.ScopeType.TUPLE);
        for (int i = 0; i < size; i++) {
            Selection<?> selection = selections.get(i);
            BaseQueryExportSelection exportSelection = exportMap
                    .get(realBaseTable)
                    .selectionOrNull(i, rootRealTable(selection, builder));
            if (selection instanceof Expression<?>) {
                builder.separator().sql("c").sql(Integer.toString(exportSelection.expressionIndex()));
            } else {
                for (BaseQueryExportColumn column : exportSelection.columns()) {
                    builder.separator().sql("c").sql(Integer.toString(column.getIndex()));
                }
            }
        }
        builder.leave();
    }

    private static RealTable rootRealTable(Selection<?> selection, SqlBuilder builder) {
        if (!(selection instanceof Table<?>)) {
            return null;
        }
        AstContext astContext = builder.getAstContext();
        QueryRenderContext renderContext = builder.getQueryRenderContext();
        return renderContext != null ?
                TableProxies.resolve((Table<?>) selection, astContext).realTable(renderContext) :
                TableProxies.resolve((Table<?>) selection, astContext).realTable(astContext);
    }

    private static RealTable childTableByKeys(RealTable table, List<RealTable.Key> keys) {
        for (RealTable.Key key : keys) {
            table = table.child(key);
        }
        return table;
    }
}
