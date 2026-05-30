package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.sql.ast.impl.table.TableUtils;
import org.babyfish.jimmer.sql.ast.query.ConfigurableBaseQuery;
import org.babyfish.jimmer.sql.ast.table.spi.TableLike;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

import static java.util.Collections.emptyMap;

public final class BaseQueryExports {

    public static final BaseQueryExports EMPTY = new BaseQueryExports(emptyMap(), emptyMap(), emptyMap());

    private final Map<ConfigurableBaseQuery<?>, BaseQueryExportResolver> resolverMapByQuery;

    private final Map<BaseTableSymbol, BaseQueryExportResolver> resolverMapByBaseTable;

    private final Map<ConfigurableBaseQuery<?>, BaseTableSymbol> baseTableMapByQuery;

    BaseQueryExports(
            Map<ConfigurableBaseQuery<?>, BaseQueryExportResolver> resolverMapByQuery,
            Map<BaseTableSymbol, BaseQueryExportResolver> resolverMapByBaseTable,
            Map<ConfigurableBaseQuery<?>, BaseTableSymbol> baseTableMapByQuery
    ) {
        this.resolverMapByQuery = resolverMapByQuery;
        this.resolverMapByBaseTable = resolverMapByBaseTable;
        this.baseTableMapByQuery = baseTableMapByQuery;
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
        BaseQueryExportResolver resolver = resolver(baseTable);
        return resolver != null ? resolver.exportSelectionOrNull(baseTableOwner) : null;
    }

    @Nullable
    public BaseSelectionAliasRender baseSelectionRender(ConfigurableBaseQuery<?> query) {
        BaseQueryExportResolver resolver = resolverMapByQuery.get(query);
        BaseTableSymbol baseTable = baseTableMapByQuery.get(query);
        return resolver != null && baseTable != null ? resolver.baseSelectionRender(baseTable) : null;
    }

    private BaseQueryExportResolver resolver(BaseTableSymbol baseTable) {
        BaseQueryExportResolver resolver = resolverMapByBaseTable.get(baseTable);
        if (resolver != null) {
            return resolver;
        }
        for (TableLike<?> parent = baseTable.getParent(); parent != null; parent = TableUtils.parent(parent)) {
            if (parent instanceof BaseTableSymbol) {
                resolver = resolverMapByBaseTable.get(parent);
                if (resolver != null) {
                    return resolver;
                }
            }
        }
        return null;
    }
}
