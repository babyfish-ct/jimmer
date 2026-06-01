package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
import org.babyfish.jimmer.sql.ast.impl.query.ConfigurableBaseQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.query.MergedBaseQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.query.QueryAnalysisContext;
import org.babyfish.jimmer.sql.ast.query.ConfigurableBaseQuery;
import org.babyfish.jimmer.sql.ast.impl.table.TableLikeImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.TableUtils;
import org.babyfish.jimmer.sql.ast.table.spi.TableLike;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
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
        BaseQueryScope scope = scope(baseTable);
        if (scope == null) {
            return null;
        }
        register(baseTable, scope);
        MergedBaseQueryImpl<?> mergedBy = MergedBaseQueryImpl.from(baseTable.getQuery());
        if (mergedBy != null) {
            boolean cte = baseTable.isCte();
            for (ConfigurableBaseQueryImpl<?> itemQuery : mergedBy.getExpandedQueries()) {
                BaseTableSymbol itemBaseTable = mergedBy.itemBaseTable(itemQuery, cte);
                register(itemBaseTable, scope);
                scopeMapByQuery.put(itemQuery, scope);
                scope.requireExportSelection(new BaseTableOwner(itemBaseTable, baseTableOwner.getIndex()));
            }
        }
        return scope.requireExportSelection(baseTableOwner);
    }

    public void requireExpressionIndex(BaseTableOwner baseTableOwner) {
        BaseQueryScope scope = scope(baseTableOwner);
        if (scope == null) {
            return;
        }
        for (BaseTableOwner owner : expandedOwners(baseTableOwner)) {
            register(owner.getBaseTable(), scope);
            scope.requireExportSelection(owner).requireExpressionIndex();
        }
    }

    public BaseQueryExports toExports() {
        synchronizeMergedExports();
        Map<BaseQueryScope, BaseQueryExportResolver> resolverMap = new IdentityHashMap<>();
        Map<ConfigurableBaseQuery<?>, BaseQueryExportResolver> resolverMapByQuery = new IdentityHashMap<>();
        Map<ConfigurableBaseQuery<?>, BaseTableSymbol> baseTableMapByQuery = new IdentityHashMap<>();
        for (Map.Entry<ConfigurableBaseQuery<?>, BaseQueryScope> e : scopeMapByQuery.entrySet()) {
            resolverMapByQuery.put(e.getKey(), resolver(e.getValue(), resolverMap));
        }
        Map<BaseTableSymbol, BaseQueryExportResolver> resolverMapByBaseTable = new IdentityHashMap<>();
        for (Map.Entry<BaseTableSymbol, BaseQueryScope> e : scopeMapByBaseTable.entrySet()) {
            BaseTableSymbol baseTable = e.getKey();
            resolverMapByBaseTable.put(baseTable, resolver(e.getValue(), resolverMap));
            baseTableMapByQuery.put(baseTable.getQuery(), baseTable);
        }
        return new BaseQueryExports(resolverMapByQuery, resolverMapByBaseTable, baseTableMapByQuery);
    }

    private void register(TableLikeImplementor<?> tableLikeImplementor, BaseQueryScope scope) {
        if (tableLikeImplementor instanceof BaseTableImplementor) {
            register(((BaseTableImplementor) tableLikeImplementor).toSymbol(), scope);
        }
        if (tableLikeImplementor.hasBaseTable() && tableLikeImplementor instanceof Iterable<?>) {
            for (Object child : (Iterable<?>) tableLikeImplementor) {
                if (!(child instanceof TableLikeImplementor<?>)) {
                    continue;
                }
                register((TableLikeImplementor<?>) child, scope);
            }
        }
    }

    private void register(BaseTableSymbol baseTable, BaseQueryScope scope) {
        scopeMapByBaseTable.put(baseTable, scope);
        scopeMapByQuery.put(baseTable.getQuery(), scope);
        MergedBaseQueryImpl<?> mergedBy = MergedBaseQueryImpl.from(baseTable.getQuery());
        if (mergedBy != null) {
            boolean cte = baseTable.isCte();
            for (ConfigurableBaseQueryImpl<?> itemQuery : mergedBy.getExpandedQueries()) {
                BaseTableSymbol itemBaseTable = mergedBy.itemBaseTable(itemQuery, cte);
                scopeMapByBaseTable.put(itemBaseTable, scope);
                scopeMapByQuery.put(itemQuery, scope);
            }
        }
    }

    private BaseQueryScope scope(AbstractMutableStatementImpl statement) {
        return scopeMap.computeIfAbsent(statement, it -> new BaseQueryScope(ctx));
    }

    private BaseQueryScope scope(BaseTableOwner baseTableOwner) {
        BaseTableSymbol recursive = baseTableOwner.getBaseTable().getRecursive();
        if (recursive != null) {
            baseTableOwner = new BaseTableOwner(recursive, baseTableOwner.getIndex());
        }
        return scope(baseTableOwner.getBaseTable());
    }

    private BaseQueryScope scope(BaseTableSymbol baseTable) {
        BaseQueryScope scope = scopeMapByBaseTable.get(baseTable);
        if (scope != null) {
            return scope;
        }
        if (ctx.resolveBaseTable(baseTable) != null) {
            scope = scope(ctx.getStatement());
            register(baseTable, scope);
            return scope;
        }
        for (TableLike<?> parent = baseTable.getParent(); parent != null; parent = TableUtils.parent(parent)) {
            if (parent instanceof BaseTableSymbol) {
                scope = scopeMapByBaseTable.get(parent);
                if (scope != null) {
                    return scope;
                }
            }
        }
        return null;
    }

    private void synchronizeMergedExports() {
        Map<BaseTableSymbol, Boolean> synchronizedMap = new IdentityHashMap<>();
        for (Map.Entry<BaseTableSymbol, BaseQueryScope> e : scopeMapByBaseTable.entrySet()) {
            BaseTableSymbol baseTable = e.getKey();
            MergedBaseQueryImpl<?> mergedBy = MergedBaseQueryImpl.from(baseTable.getQuery());
            if (mergedBy == null || synchronizedMap.containsKey(baseTable)) {
                continue;
            }
            List<BaseTableSymbol> baseTables = mergedBaseTables(baseTable, mergedBy);
            for (BaseTableSymbol item : baseTables) {
                synchronizedMap.put(item, Boolean.TRUE);
            }
            e.getValue().synchronizeMergedExports(baseTables);
        }
    }

    private static List<BaseTableSymbol> mergedBaseTables(
            BaseTableSymbol baseTable,
            MergedBaseQueryImpl<?> mergedBy
    ) {
        List<BaseTableSymbol> baseTables = new ArrayList<>();
        baseTables.add(baseTable);
        boolean cte = baseTable.isCte();
        for (ConfigurableBaseQueryImpl<?> itemQuery : mergedBy.getExpandedQueries()) {
            baseTables.add(mergedBy.itemBaseTable(itemQuery, cte));
        }
        return baseTables;
    }

    private static List<BaseTableOwner> expandedOwners(BaseTableOwner baseTableOwner) {
        BaseTableSymbol baseTable = baseTableOwner.getBaseTable();
        MergedBaseQueryImpl<?> mergedBy = MergedBaseQueryImpl.from(baseTable.getQuery());
        if (mergedBy == null) {
            return java.util.Collections.singletonList(baseTableOwner);
        }
        List<BaseTableOwner> owners = new ArrayList<>();
        for (BaseTableSymbol itemBaseTable : mergedBaseTables(baseTable, mergedBy)) {
            owners.add(
                    new BaseTableOwner(
                            itemBaseTable,
                            baseTableOwner.getIndex()
                    )
            );
        }
        return owners;
    }

    private static BaseQueryExportResolver resolver(
            BaseQueryScope scope,
            Map<BaseQueryScope, BaseQueryExportResolver> resolverMap
    ) {
        return resolverMap.computeIfAbsent(scope, BaseQueryScope::toResolver);
    }
}
