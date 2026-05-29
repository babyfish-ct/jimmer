package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.query.QueryAnalysisContext;
import org.babyfish.jimmer.sql.ast.impl.table.RealTable;
import org.babyfish.jimmer.sql.ast.table.Table;

import java.util.LinkedHashMap;
import java.util.Map;

final class BaseQueryExportResolver {

    private final QueryAnalysisContext ctx;

    private final Map<RealTable, BaseQueryExport> exportMap;

    BaseQueryExportResolver(QueryAnalysisContext ctx, Map<RealTable, BaseQueryExport> exportMap) {
        this.ctx = ctx;
        this.exportMap = new LinkedHashMap<>(exportMap);
    }

    BaseQueryExportSelection exportSelectionOrNull(BaseTableOwner baseTableOwner) {
        BaseTableImplementor baseTable = ctx.resolveBaseTable(baseTableOwner.getBaseTable());
        RealTable realBaseTable = ctx.realTable(baseTable);
        BaseQueryExport export = exportMap.get(realBaseTable);
        return export != null ?
                export.selectionOrNull(baseTableOwner.index, rootRealTable(baseTable, baseTableOwner.index)) :
                null;
    }

    BaseSelectionAliasRender baseSelectionRender(BaseTableSymbol baseTableSymbol) {
        return new BaseSelectionAliasRenderer(exportMap, baseTableSymbol);
    }

    private RealTable rootRealTable(BaseTableImplementor baseTable, int selectionIndex) {
        Selection<?> selection = baseTable.getSelections().get(selectionIndex);
        if (!(selection instanceof Table<?>)) {
            return null;
        }
        return ctx.realTable(ctx.resolve((Table<?>) selection));
    }
}
