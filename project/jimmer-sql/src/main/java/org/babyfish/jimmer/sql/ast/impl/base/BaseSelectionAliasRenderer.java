package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.impl.util.Classes;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.Ast;
import org.babyfish.jimmer.sql.ast.impl.ExpressionImplementor;
import org.babyfish.jimmer.sql.ast.impl.query.ConfigurableBaseQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.table.RealTable;
import org.babyfish.jimmer.sql.ast.impl.table.TableProxies;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

import java.util.List;
import java.util.Map;

final class BaseSelectionAliasRenderer implements BaseSelectionAliasRender {

    private final Map<BaseTableSymbol, BaseQueryExport> exportMap;

    private final Map<BaseTableSymbol, BaseTableSymbol> canonicalBaseTableMap;

    private final boolean cte;

    BaseSelectionAliasRenderer(
            Map<BaseTableSymbol, BaseQueryExport> exportMap,
            Map<BaseTableSymbol, BaseTableSymbol> canonicalBaseTableMap,
            BaseTableSymbol baseTableSymbol
    ) {
        this.exportMap = exportMap;
        this.canonicalBaseTableMap = canonicalBaseTableMap;
        this.cte = baseTableSymbol.isCte();
    }

    @Override
    public void render(int index, Selection<?> selection, SqlBuilder builder) {
        RealTable realBaseTable = builder.getAstContext().getRenderedRealBaseTable();
        BaseQueryExport export = export(
                ((BaseTableImplementor) realBaseTable.getTableLikeImplementor()).toSymbol()
        );
        if (export == null || export.isEmpty()) {
            if (index == 0) {
                builder.separator().sql(cte ? "1" : "1 as c0");
            }
            return;
        }
        BaseQueryExportSelection exportSelection = export.selectionOrNull(index);
        if (exportSelection == null) {
            return;
        }

        if (selection instanceof Expression<?>) {
            builder.separator();
            ConfigurableBaseQueryImpl<?> query =
                    ((BaseTableImplementor) realBaseTable.getTableLikeImplementor()).toSymbol().getQuery();
            boolean rootless = query.getMutableQuery().getTableLikeImplementor() == null;
            String castType = null;
            if (rootless) {
                castType = builder.sqlClient().getDialect().getRootlessSelectionCastType(
                        Classes.primitiveTypeOf(((ExpressionImplementor<?>) selection).getType())
                );
            }
            if (castType != null) {
                builder.sql("cast(");
            }
            ((Ast) selection).renderTo(builder);
            if (castType != null) {
                builder.sql(" as ").sql(castType).sql(")");
            }
            if (!cte) {
                builder.sql(
                        rootless ?
                                " as c" :
                                " c"
                ).sql(Integer.toString(exportSelection.expressionIndex()));
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
            String alias = builder.alias(childTable);
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
        BaseTableImplementor baseTableImplementor =
                (BaseTableImplementor) realBaseTable.getTableLikeImplementor();
        ConfigurableBaseQueryImpl<?> query = baseTableImplementor.toSymbol().getQuery();
        List<Selection<?>> selections = query.getSelections();
        int size = selections.size();
        BaseQueryExport export = export(baseTableImplementor.toSymbol());
        builder.enter(AbstractSqlBuilder.ScopeType.TUPLE);
        if (export == null || export.isEmpty()) {
            builder.sql("c0").leave();
            return;
        }
        for (int i = 0; i < size; i++) {
            Selection<?> selection = selections.get(i);
            BaseQueryExportSelection exportSelection = export.selectionOrNull(i);
            if (exportSelection == null) {
                continue;
            }
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

    private static RealTable childTableByKeys(RealTable table, List<RealTable.Key> keys) {
        for (RealTable.Key key : keys) {
            table = table.child(key);
        }
        return table;
    }

    private BaseQueryExport export(BaseTableSymbol baseTable) {
        BaseTableSymbol canonical = canonicalBaseTableMap.get(baseTable);
        return exportMap.get(canonical != null ? canonical : baseTable);
    }
}
