package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.table.RealTable;
import org.babyfish.jimmer.sql.ast.impl.table.TableProxies;
import org.babyfish.jimmer.sql.ast.table.Table;

import java.util.LinkedHashMap;
import java.util.Map;

final class BaseQueryExportResolver {

    private final AstContext astContext;

    private final Map<RealTable, BaseQueryExport> exportMap;

    BaseQueryExportResolver(AstContext astContext, Map<RealTable, BaseQueryExport> exportMap) {
        this.astContext = astContext;
        this.exportMap = new LinkedHashMap<>(exportMap);
    }

    BaseQueryExportSelection exportSelectionOrNull(BaseTableOwner baseTableOwner) {
        BaseTableImplementor baseTable = astContext.resolveBaseTable(baseTableOwner.getBaseTable());
        RealTable realBaseTable = baseTable.realTable(astContext);
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
        return TableProxies
                .resolve((Table<?>) selection, astContext)
                .realTable(astContext);
    }
}
