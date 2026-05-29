package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.query.MergedBaseQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.query.TypedBaseQueryImplementor;

import java.util.IdentityHashMap;
import java.util.Map;

public final class BaseQueryExportsCollector {

    private final AstContext astContext;

    private final Map<AbstractMutableStatementImpl, BaseQueryScope> scopeMap =
            new IdentityHashMap<>();

    public BaseQueryExportsCollector(AstContext astContext) {
        this.astContext = astContext;
    }

    public BaseQueryExportCollectorSelection exportSelection(BaseTableOwner baseTableOwner) {
        BaseTableSymbol recursive = baseTableOwner.getBaseTable().getRecursive();
        if (recursive != null) {
            baseTableOwner = new BaseTableOwner(recursive, baseTableOwner.getIndex());
        }
        BaseTableSymbol baseTable = baseTableOwner.getBaseTable();
        AbstractMutableStatementImpl statement = astContext.findBaseQueryStatement(baseTable);
        if (statement == null) {
            return null;
        }
        BaseQueryScope scope = scope(statement);
        MergedBaseQueryImpl<?> mergedBy = MergedBaseQueryImpl.from(baseTable.getQuery());
        if (mergedBy != null) {
            boolean cte = baseTable.isCte();
            for (TypedBaseQueryImplementor<?> itemQuery : mergedBy.getExpandedQueries()) {
                scope.requireExportSelection(new BaseTableOwner(itemQuery.asBaseTable(null, cte), baseTableOwner.getIndex()));
            }
        }
        return scope.requireExportSelection(baseTableOwner);
    }

    public BaseQueryExports toExports() {
        return new BaseQueryExports(astContext, new IdentityHashMap<>(scopeMap));
    }

    private BaseQueryScope scope(AbstractMutableStatementImpl statement) {
        return scopeMap.computeIfAbsent(
                statement,
                it -> new BaseQueryScope(astContext)
        );
    }
}
