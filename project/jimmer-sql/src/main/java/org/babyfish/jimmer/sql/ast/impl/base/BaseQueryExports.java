package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.sql.ast.impl.query.ConfigurableBaseQueryImpl;
import org.babyfish.jimmer.sql.ast.query.ConfigurableBaseQuery;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public final class BaseQueryExports {

    private final Map<ConfigurableBaseQuery<?>, BaseQueryExportResolver> resolverMapByQuery;

    private final Map<BaseTableSymbol, BaseQueryExportResolver> resolverMapByBaseTable;

    BaseQueryExports(
            Map<ConfigurableBaseQuery<?>, BaseQueryExportResolver> resolverMapByQuery,
            Map<BaseTableSymbol, BaseQueryExportResolver> resolverMapByBaseTable
    ) {
        this.resolverMapByQuery = resolverMapByQuery;
        this.resolverMapByBaseTable = resolverMapByBaseTable;
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
        BaseQueryExportResolver resolver = resolverMapByBaseTable.get(baseTable);
        return resolver != null ? resolver.exportSelectionOrNull(baseTableOwner) : null;
    }

    @Nullable
    public BaseSelectionAliasRender baseSelectionRender(ConfigurableBaseQuery<?> query) {
        BaseQueryExportResolver resolver = resolverMapByQuery.get(query);
        return resolver != null ?
                resolver.baseSelectionRender((BaseTableSymbol) ((ConfigurableBaseQueryImpl<?>) query).getBaseTable()) :
                null;
    }
}
