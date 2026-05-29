package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.query.MergedBaseQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.query.TypedBaseQueryImplementor;
import org.babyfish.jimmer.sql.ast.query.ConfigurableBaseQuery;
import org.jetbrains.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.Map;

public final class BaseQueryExportPlan {

    private final AstContext astContext;

    private final Map<AbstractMutableStatementImpl, BaseQueryScope> scopeMap =
            new IdentityHashMap<>();

    public BaseQueryExportPlan(AstContext astContext) {
        this.astContext = astContext;
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
        AbstractMutableStatementImpl statement = astContext.findBaseQueryStatement(baseTable);
        if (statement == null) {
            return null;
        }
        BaseQueryScope scope = scope(statement);
        MergedBaseQueryImpl<?> mergedBy = MergedBaseQueryImpl.from(baseTable.getQuery());
        if (mergedBy != null) {
            boolean cte = baseTable.isCte();
            for (TypedBaseQueryImplementor<?> itemQuery : mergedBy.getExpandedQueries()) {
                scope.exportSelection(new BaseTableOwner(itemQuery.asBaseTable(null, cte), baseTableOwner.getIndex()));
            }
        }
        return scope.exportSelection(baseTableOwner);
    }

    @Nullable
    public BaseSelectionAliasRender baseSelectionRender(ConfigurableBaseQuery<?> query) {
        AbstractMutableStatementImpl statement = astContext.findCurrentStatementUsingBaseQuery();
        return statement != null ?
                scope(statement).toBaseSelectionRender(query) :
                null;
    }

    public void freeze() {
        for (BaseQueryScope scope : scopeMap.values()) {
            scope.freeze();
        }
    }

    private BaseQueryScope scope(AbstractMutableStatementImpl statement) {
        return scopeMap.computeIfAbsent(
                statement,
                it -> new BaseQueryScope(astContext)
        );
    }
}
