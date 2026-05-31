package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.base.BaseQueryExportSelection;
import org.babyfish.jimmer.sql.ast.impl.base.BaseSelectionAliasRender;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableOwner;
import org.babyfish.jimmer.sql.ast.impl.table.RealTable;
import org.babyfish.jimmer.sql.ast.impl.table.TableAliasScope;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.query.ConfigurableBaseQuery;
import org.jetbrains.annotations.Nullable;

public final class QueryAnalysis {

    private final AstContext astContext;

    private final QueryAnalysisModel model;

    QueryAnalysis(AstContext astContext, QueryAnalysisModel model) {
        this.astContext = astContext;
        this.model = model;
    }

    public AstContext getAstContext() {
        return astContext;
    }

    @Nullable
    public BaseQueryExportSelection getBaseQueryExportSelection(BaseTableOwner baseTableOwner) {
        return model.getBaseQueryExports().exportSelection(baseTableOwner);
    }

    @Nullable
    public BaseSelectionAliasRender getBaseSelectionRender(ConfigurableBaseQuery<?> query) {
        return model.getBaseQueryExports().baseSelectionRender(query);
    }

    @Nullable
    public JoinType getRequiredJoinType(TableImplementor<?> table) {
        return model.getJoinRequirements().get(table);
    }

    public void applyAliases(RealTable table, TableAliasScope aliasScope) {
        if (aliasScope != null) {
            aliasScope.applyAliases(table, model.getTableAliases());
        }
    }
}
