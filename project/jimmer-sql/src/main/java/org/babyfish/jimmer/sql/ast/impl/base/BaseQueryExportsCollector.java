package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
import org.babyfish.jimmer.sql.ast.impl.table.TableLikeImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.TableUtils;
import org.babyfish.jimmer.sql.ast.impl.query.ConfigurableBaseQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.query.MergedBaseQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.query.QueryAnalysisContext;
import org.babyfish.jimmer.sql.ast.query.ConfigurableBaseQuery;
import org.babyfish.jimmer.sql.ast.table.spi.TableLike;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class BaseQueryExportsCollector {

    private final QueryAnalysisContext ctx;

    @Nullable
    private State state;

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
                state().scopeMapByQuery.put(itemQuery, scope);
                scope.requireExportSelection(new BaseTableOwner(itemBaseTable, baseTableOwner.getIndex()));
            }
        }
        return scope.requireExportSelection(baseTableOwner);
    }

    public BaseQueryExports toExports(
            Map<BaseTableSymbol, BaseTableSymbol> canonicalBaseTableMap
    ) {
        State state = this.state;
        if (state == null) {
            return BaseQueryExports.EMPTY;
        }
        synchronizeMergedExports(state);
        Map<BaseQueryScope, BaseQueryExportResolver> resolverMap = new IdentityHashMap<>();
        for (BaseQueryScope scope : state.scopeMap.values()) {
            scope.prepareExports();
            resolverMap.put(scope, new BaseQueryExportResolver());
        }
        Map<ConfigurableBaseQuery<?>, BaseTableSymbol> baseTableMapByQuery = new IdentityHashMap<>();
        Map<BaseTableSymbol, BaseQueryExportResolver> resolverMapByBaseTable = new IdentityHashMap<>();
        for (Map.Entry<BaseTableSymbol, BaseQueryScope> e : state.scopeMapByBaseTable.entrySet()) {
            BaseTableSymbol baseTable = e.getKey();
            BaseQueryExportResolver resolver = resolverMap.get(e.getValue());
            resolverMapByBaseTable.put(baseTable, resolver);
            BaseQueryExport export = e.getValue().exportOrNull(baseTable);
            if (export != null) {
                resolver.put(baseTable, export);
            }
            baseTableMapByQuery.put(baseTable.getQuery(), baseTable);
        }
        Map<ConfigurableBaseQuery<?>, BaseSelectionAliasRender> renderMapByQuery = new IdentityHashMap<>();
        for (Map.Entry<ConfigurableBaseQuery<?>, BaseQueryScope> e : state.scopeMapByQuery.entrySet()) {
            BaseTableSymbol baseTable = baseTableMapByQuery.get(e.getKey());
            if (baseTable != null) {
                renderMapByQuery.put(
                        e.getKey(),
                        resolverMap.get(e.getValue()).baseSelectionRender(
                                baseTable,
                                canonicalBaseTableMap
                        )
                );
            }
        }
        return new BaseQueryExports(
                renderMapByQuery,
                resolverMapByBaseTable,
                canonicalBaseTableMap
        );
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
        State state = state();
        state.scopeMapByBaseTable.put(baseTable, scope);
        state.scopeMapByQuery.put(baseTable.getQuery(), scope);
        MergedBaseQueryImpl<?> mergedBy = MergedBaseQueryImpl.from(baseTable.getQuery());
        if (mergedBy != null) {
            boolean cte = baseTable.isCte();
            for (ConfigurableBaseQueryImpl<?> itemQuery : mergedBy.getExpandedQueries()) {
                BaseTableSymbol itemBaseTable = mergedBy.itemBaseTable(itemQuery, cte);
                state.scopeMapByBaseTable.put(itemBaseTable, scope);
                state.scopeMapByQuery.put(itemQuery, scope);
            }
        }
    }

    private BaseQueryScope scope(AbstractMutableStatementImpl statement) {
        return state().scopeMap.computeIfAbsent(statement, it -> new BaseQueryScope(ctx));
    }

    private BaseQueryScope scope(BaseTableOwner baseTableOwner) {
        BaseTableSymbol recursive = baseTableOwner.getBaseTable().getRecursive();
        if (recursive != null) {
            baseTableOwner = new BaseTableOwner(recursive, baseTableOwner.getIndex());
        }
        return scope(baseTableOwner.getBaseTable());
    }

    private BaseQueryScope scope(BaseTableSymbol baseTable) {
        State state = this.state;
        BaseQueryScope scope = state != null ? state.scopeMapByBaseTable.get(baseTable) : null;
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
                scope = state != null ? state.scopeMapByBaseTable.get(parent) : null;
                if (scope != null) {
                    return scope;
                }
            }
        }
        return null;
    }

    private static void synchronizeMergedExports(State state) {
        Map<BaseTableSymbol, Boolean> synchronizedMap = new IdentityHashMap<>();
        for (Map.Entry<BaseTableSymbol, BaseQueryScope> e : state.scopeMapByBaseTable.entrySet()) {
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

    private State state() {
        State state = this.state;
        if (state == null) {
            state = this.state = new State();
        }
        return state;
    }

    private static class State {

        final Map<AbstractMutableStatementImpl, BaseQueryScope> scopeMap = new IdentityHashMap<>();

        final Map<ConfigurableBaseQuery<?>, BaseQueryScope> scopeMapByQuery = new IdentityHashMap<>();

        final Map<BaseTableSymbol, BaseQueryScope> scopeMapByBaseTable = new IdentityHashMap<>();
    }
}
