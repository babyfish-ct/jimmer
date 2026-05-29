package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.query.ConfigurableBaseQuery;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public final class BaseQueryExports {

    private final AstContext astContext;

    private final Map<AbstractMutableStatementImpl, BaseQueryScope> scopeMap;

    BaseQueryExports(
            AstContext astContext,
            Map<AbstractMutableStatementImpl, BaseQueryScope> scopeMap
    ) {
        this.astContext = astContext;
        this.scopeMap = scopeMap;
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
        BaseQueryScope scope = scopeMap.get(statement);
        return scope != null ? scope.exportSelectionOrNull(baseTableOwner) : null;
    }

    @Nullable
    public BaseSelectionAliasRender baseSelectionRender(ConfigurableBaseQuery<?> query) {
        AbstractMutableStatementImpl statement = astContext.findCurrentStatementUsingBaseQuery();
        if (statement == null) {
            return null;
        }
        BaseQueryScope scope = scopeMap.get(statement);
        return scope != null ? scope.toBaseSelectionRender(query) : null;
    }
}
