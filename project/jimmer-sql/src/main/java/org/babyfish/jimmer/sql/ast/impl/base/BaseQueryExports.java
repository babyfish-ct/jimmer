package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.sql.ast.query.ConfigurableBaseQuery;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public final class BaseQueryExports {

    private final Map<ConfigurableBaseQuery<?>, BaseQueryScope> scopeMapByQuery;

    private final Map<BaseTableSymbol, BaseQueryScope> scopeMapByBaseTable;

    BaseQueryExports(
            Map<ConfigurableBaseQuery<?>, BaseQueryScope> scopeMapByQuery,
            Map<BaseTableSymbol, BaseQueryScope> scopeMapByBaseTable
    ) {
        this.scopeMapByQuery = scopeMapByQuery;
        this.scopeMapByBaseTable = scopeMapByBaseTable;
    }

    @Nullable
    public BaseQueryExportSelection exportSelection(BaseTableOwner baseTableOwner) {
        if (baseTableOwner == null) {
            return null;
        }
        BaseTableSymbol recursive = baseTableOwner.getBaseTable().getRecursive();
        if (recursive != null) {
            baseTableOwner = new BaseTableOwner(recursive, baseTableOwner.getIndex());
        }
        BaseTableSymbol baseTable = baseTableOwner.getBaseTable();
        BaseQueryScope scope = scopeMapByBaseTable.get(baseTable);
        return scope != null ? scope.exportSelectionOrNull(baseTableOwner) : null;
    }

    @Nullable
    public BaseSelectionAliasRender baseSelectionRender(ConfigurableBaseQuery<?> query) {
        BaseQueryScope scope = scopeMapByQuery.get(query);
        return scope != null ? scope.toBaseSelectionRender(query) : null;
    }
}
