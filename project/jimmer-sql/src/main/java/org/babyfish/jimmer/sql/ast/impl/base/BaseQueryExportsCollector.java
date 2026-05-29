package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.query.MergedBaseQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.query.TypedBaseQueryImplementor;
import org.babyfish.jimmer.sql.ast.query.ConfigurableBaseQuery;
import org.babyfish.jimmer.sql.ast.impl.table.TableLikeImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.TableUtils;

import java.util.IdentityHashMap;
import java.util.Map;

public final class BaseQueryExportsCollector {

    private final AstContext astContext;

    private final Map<AbstractMutableStatementImpl, BaseQueryScope> scopeMap =
            new IdentityHashMap<>();

    private final Map<ConfigurableBaseQuery<?>, BaseQueryScope> scopeMapByQuery =
            new IdentityHashMap<>();

    private final Map<BaseTableSymbol, BaseQueryScope> scopeMapByBaseTable =
            new IdentityHashMap<>();

    public BaseQueryExportsCollector(AstContext astContext) {
        this.astContext = astContext;
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
        return new BaseQueryExports(
                new IdentityHashMap<>(scopeMapByQuery),
                new IdentityHashMap<>(scopeMapByBaseTable)
        );
    }

    private void register(TableLikeImplementor<?> tableLikeImplementor, BaseQueryScope scope) {
        if (tableLikeImplementor instanceof BaseTableImplementor) {
            register(((BaseTableImplementor) tableLikeImplementor).toSymbol(), scope);
        } else if (tableLikeImplementor.hasBaseTable()) {
            Iterable<TableLikeImplementor<?>> children =
                    (Iterable<TableLikeImplementor<?>>) tableLikeImplementor;
            for (TableLikeImplementor<?> child : children) {
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
        return scopeMap.computeIfAbsent(
                statement,
                it -> new BaseQueryScope(astContext)
        );
    }
}
