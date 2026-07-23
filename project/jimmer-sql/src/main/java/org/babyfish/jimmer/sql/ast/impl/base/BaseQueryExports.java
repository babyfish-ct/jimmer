package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.sql.ast.impl.table.TableUtils;
import org.babyfish.jimmer.sql.ast.query.ConfigurableBaseQuery;
import org.babyfish.jimmer.sql.ast.table.spi.TableLike;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

import static java.util.Collections.emptyMap;

public final class BaseQueryExports {

    public static final BaseQueryExports EMPTY =
            new BaseQueryExports(emptyMap(), emptyMap(), emptyMap());

    private final Map<ConfigurableBaseQuery<?>, BaseSelectionAliasRender> renderMapByQuery;

    private final Map<BaseTableSymbol, BaseQueryExportResolver> resolverMapByBaseTable;

    private final Map<BaseTableSymbol, BaseTableSymbol> canonicalBaseTableMap;

    BaseQueryExports(
            Map<ConfigurableBaseQuery<?>, BaseSelectionAliasRender> renderMapByQuery,
            Map<BaseTableSymbol, BaseQueryExportResolver> resolverMapByBaseTable,
            Map<BaseTableSymbol, BaseTableSymbol> canonicalBaseTableMap
    ) {
        this.renderMapByQuery = renderMapByQuery;
        this.resolverMapByBaseTable = resolverMapByBaseTable;
        this.canonicalBaseTableMap = canonicalBaseTableMap;
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
        BaseTableSymbol baseTable = canonical(baseTableOwner.getBaseTable());
        if (baseTable != baseTableOwner.getBaseTable()) {
            baseTableOwner = new BaseTableOwner(baseTable, baseTableOwner.getIndex());
        }
        BaseQueryExportResolver resolver = resolver(baseTable);
        return resolver != null ? resolver.exportSelectionOrNull(baseTableOwner) : null;
    }

    @Nullable
    public BaseSelectionAliasRender baseSelectionRender(ConfigurableBaseQuery<?> query) {
        return renderMapByQuery.get(query);
    }

    private BaseQueryExportResolver resolver(BaseTableSymbol baseTable) {
        BaseQueryExportResolver resolver = resolverMapByBaseTable.get(baseTable);
        if (resolver != null) {
            return resolver;
        }
        for (TableLike<?> parent = baseTable.getParent(); parent != null; parent = TableUtils.parent(parent)) {
            if (parent instanceof BaseTableSymbol) {
                resolver = resolverMapByBaseTable.get(canonical((BaseTableSymbol) parent));
                if (resolver != null) {
                    return resolver;
                }
            }
        }
        return null;
    }

    private BaseTableSymbol canonical(BaseTableSymbol baseTable) {
        BaseTableSymbol canonical = canonicalBaseTableMap.get(baseTable);
        return canonical != null ? canonical : baseTable;
    }
}
