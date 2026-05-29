package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.Ast;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.query.ConfigurableBaseQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.table.RealTable;
import org.babyfish.jimmer.sql.ast.impl.table.TableProxies;
import org.babyfish.jimmer.sql.ast.query.ConfigurableBaseQuery;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

import java.util.*;

public class BaseQueryScope {

    final AstContext astContext;

    private final Map<RealTable, BaseQueryExport> exportMap =
            new LinkedHashMap<>();

    private int colNoSequence;

    public BaseQueryScope(AstContext astContext) {
        this.astContext = astContext;
    }

    BaseQueryExportCollectorSelection requireExportSelection(BaseTableOwner baseTableOwner) {
        BaseTableImplementor baseTable = astContext.resolveBaseTable(baseTableOwner.getBaseTable());
        RealTable realBaseTable = baseTable.realTable(astContext);
        BaseQueryExport export = exportMap.get(realBaseTable);
        if (export == null) {
            export = new BaseQueryExport(this, realBaseTable);
            exportMap.put(realBaseTable, export);
        }
        return export.requireSelection(baseTableOwner.index);
    }

    BaseQueryExportSelection exportSelectionOrNull(BaseTableOwner baseTableOwner) {
        BaseTableImplementor baseTable = astContext.resolveBaseTable(baseTableOwner.getBaseTable());
        RealTable realBaseTable = baseTable.realTable(astContext);
        BaseQueryExport export = exportMap.get(realBaseTable);
        return export != null ? export.selectionOrNull(baseTableOwner.index) : null;
    }

    int colNo() {
        return ++colNoSequence;
    }

    public BaseSelectionAliasRender toBaseSelectionRender(ConfigurableBaseQuery<?> query) {
        return new BaseSelectionAliasRenderImpl(
                exportMap,
                (BaseTableSymbol) ((ConfigurableBaseQueryImpl<?>)query).getBaseTable()
        );
    }

    private static class BaseSelectionAliasRenderImpl implements BaseSelectionAliasRender {

        private final Map<RealTable, BaseQueryExport> exportMap;

        private final boolean cte;

        BaseSelectionAliasRenderImpl(Map<RealTable, BaseQueryExport> exportMap, BaseTableSymbol baseTableSymbol) {
            this.exportMap = exportMap;
            this.cte = baseTableSymbol.isCte();
        }

        @Override
        public void render(int index, Selection<?> selection, SqlBuilder builder) {
            RealTable realBaseTable = builder.getAstContext().getRenderedRealBaseTable();
            BaseQueryExport export = exportMap.get(realBaseTable);
            BaseQueryExportSelection exportSelection = export != null ? export.selectionOrNull(index) : null;
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
                    .realTable(builder.getAstContext());
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
                BaseQueryExportSelection exportSelection = exportMap.get(realBaseTable).selectionOrNull(i);
                Selection<?> selection = selections.get(i);
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
    }

    private static RealTable childTableByKeys(RealTable table, List<RealTable.Key> keys) {
        for (RealTable.Key key : keys) {
            table = table.child(key);
        }
        return table;
    }
}
