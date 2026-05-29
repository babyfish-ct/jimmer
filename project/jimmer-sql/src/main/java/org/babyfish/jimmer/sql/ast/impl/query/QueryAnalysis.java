package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.base.BaseQueryExportSelection;
import org.babyfish.jimmer.sql.ast.impl.base.BaseQueryExports;
import org.babyfish.jimmer.sql.ast.impl.base.BaseSelectionAliasRender;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableOwner;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.query.ConfigurableBaseQuery;
import org.jetbrains.annotations.Nullable;

public final class QueryAnalysis {

    private final AstContext astContext;

    private final BaseQueryExports baseQueryExports;

    private final JoinRequirementPlan joinRequirementPlan;

    QueryAnalysis(
            AstContext astContext,
            BaseQueryExports baseQueryExports,
            JoinRequirementPlan joinRequirementPlan
    ) {
        this.astContext = astContext;
        this.baseQueryExports = baseQueryExports;
        this.joinRequirementPlan = joinRequirementPlan;
    }

    public AstContext getAstContext() {
        return astContext;
    }

    @Nullable
    public BaseQueryExportSelection getBaseQueryExportSelection(BaseTableOwner baseTableOwner) {
        return baseQueryExports.exportSelection(baseTableOwner);
    }

    @Nullable
    public BaseSelectionAliasRender getBaseSelectionRender(ConfigurableBaseQuery<?> query) {
        return baseQueryExports.baseSelectionRender(query);
    }

    @Nullable
    public JoinType getRequiredJoinType(TableImplementor<?> table) {
        return joinRequirementPlan.get(table);
    }
}
