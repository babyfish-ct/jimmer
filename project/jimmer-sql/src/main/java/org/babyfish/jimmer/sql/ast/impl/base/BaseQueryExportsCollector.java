package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
import org.babyfish.jimmer.sql.ast.impl.query.MergedBaseQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.query.QueryAnalysisContext;
import org.babyfish.jimmer.sql.ast.impl.query.TypedBaseQueryImplementor;
import org.babyfish.jimmer.sql.ast.query.ConfigurableBaseQuery;
import org.babyfish.jimmer.sql.ast.impl.table.TableLikeImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.TableUtils;

import java.util.IdentityHashMap;
import java.util.Map;

public final class BaseQueryExportsCollector {

    private final QueryAnalysisContext ctx;

    private final Map<AbstractMutableStatementImpl, BaseQueryScope> scopeMap = new IdentityHashMap<>();

    private final Map<ConfigurableBaseQuery<?>, BaseQueryScope> scopeMapByQuery = new IdentityHashMap<>();

    private final Map<BaseTableSymbol, BaseQueryScope> scopeMapByBaseTable = new IdentityHashMap<>();

    public BaseQueryExportsCollector(QueryAnalysisContext ctx) {
        this.ctx = ctx;
    }

    public void registerStatement(AbstractMutableStatementImpl statement) {
        TableLikeImplementor<?> tableLikeImplementor = statement.getTableLikeImplementor();
        if (!TableUtils.hasBaseTable(tableLikeImplementor)) {
            return;
        }
        BaseQueryScope scope = scope(statement);
        register(tableLikeImplementor, scope);
    }

    public BaseQueryExportCollectorSelection exportSelection(BaseTableOwner baseTableOwner) {
        BaseTableSymbol recursive = baseTableOwner.getBaseTable().getRecursive();
        if (recursive != null) {
            baseTableOwner = new BaseTableOwner(recursive, baseTableOwner.getIndex());
        }
        BaseTableSymbol baseTable = baseTableOwner.getBaseTable();
        BaseQueryScope scope = scopeMapByBaseTable.get(baseTable);
        if (scope == null) {
            return null;
        }
        MergedBaseQueryImpl<?> mergedBy = MergedBaseQueryImpl.from(baseTable.getQuery());
        if (mergedBy != null) {
            boolean cte = baseTable.isCte();
            for (TypedBaseQueryImplementor<?> itemQuery : mergedBy.getExpandedQueries()) {
                BaseTableSymbol itemBaseTable = (BaseTableSymbol) itemQuery.asBaseTable(null, cte);
                register(itemBaseTable, scope);
                scope.requireExportSelection(new BaseTableOwner(itemBaseTable, baseTableOwner.getIndex()));
            }
        }
        return scope.requireExportSelection(baseTableOwner);
    }

    public BaseQueryExports toExports() {
        Map<BaseQueryScope, BaseQueryExportResolver> resolverMap = new IdentityHashMap<>();
        Map<ConfigurableBaseQuery<?>, BaseQueryExportResolver> resolverMapByQuery = new IdentityHashMap<>();
        for (Map.Entry<ConfigurableBaseQuery<?>, BaseQueryScope> e : scopeMapByQuery.entrySet()) {
            resolverMapByQuery.put(e.getKey(), resolver(e.getValue(), resolverMap));
        }
        Map<BaseTableSymbol, BaseQueryExportResolver> resolverMapByBaseTable = new IdentityHashMap<>();
        for (Map.Entry<BaseTableSymbol, BaseQueryScope> e : scopeMapByBaseTable.entrySet()) {
            resolverMapByBaseTable.put(e.getKey(), resolver(e.getValue(), resolverMap));
        }
        return new BaseQueryExports(resolverMapByQuery, resolverMapByBaseTable);
    }

    private void register(TableLikeImplementor<?> tableLikeImplementor, BaseQueryScope scope) {
        if (tableLikeImplementor instanceof BaseTableImplementor) {
            register(((BaseTableImplementor) tableLikeImplementor).toSymbol(), scope);
        } else if (tableLikeImplementor.hasBaseTable()) {
            for (TableLikeImplementor<?> child : (Iterable<TableLikeImplementor<?>>) tableLikeImplementor) {
                register(child, scope);
            }
        }
    }

    private void register(BaseTableSymbol baseTable, BaseQueryScope scope) {
        scopeMapByBaseTable.put(baseTable, scope);
        scopeMapByQuery.put(baseTable.getQuery(), scope);
        MergedBaseQueryImpl<?> mergedBy = MergedBaseQueryImpl.from(baseTable.getQuery());
        if (mergedBy != null) {
            boolean cte = baseTable.isCte();
            for (TypedBaseQueryImplementor<?> itemQuery : mergedBy.getExpandedQueries()) {
                BaseTableSymbol itemBaseTable = (BaseTableSymbol) itemQuery.asBaseTable(null, cte);
                scopeMapByBaseTable.put(itemBaseTable, scope);
                scopeMapByQuery.put(itemBaseTable.getQuery(), scope);
            }
        }
    }

    private BaseQueryScope scope(AbstractMutableStatementImpl statement) {
        return scopeMap.computeIfAbsent(statement, it -> new BaseQueryScope(ctx));
    }

    private static BaseQueryExportResolver resolver(
            BaseQueryScope scope,
            Map<BaseQueryScope, BaseQueryExportResolver> resolverMap
    ) {
        return resolverMap.computeIfAbsent(scope, BaseQueryScope::toResolver);
    }
}
