package org.babyfish.jimmer.sql.ast.impl.base;

import java.util.IdentityHashMap;
import java.util.Map;

final class BaseQueryExportResolver {

    private final Map<BaseTableSymbol, BaseQueryExport> exportMap = new IdentityHashMap<>();

    void put(BaseTableSymbol baseTable, BaseQueryExport export) {
        exportMap.put(baseTable, export);
    }

    BaseQueryExportSelection exportSelectionOrNull(BaseTableOwner baseTableOwner) {
        BaseQueryExport export = exportMap.get(baseTableOwner.getBaseTable());
        return export != null ? export.selectionOrNull(baseTableOwner.index) : null;
    }

    BaseSelectionAliasRender baseSelectionRender(
            BaseTableSymbol baseTable,
            Map<BaseTableSymbol, BaseTableSymbol> canonicalBaseTableMap
    ) {
        return new BaseSelectionAliasRenderer(exportMap, canonicalBaseTableMap, baseTable);
    }

}
