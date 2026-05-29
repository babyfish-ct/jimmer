package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.base.BaseQueryExportSelection;
import org.babyfish.jimmer.sql.ast.impl.base.BaseSelectionAliasRender;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableOwner;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.query.ConfigurableBaseQuery;
import org.jetbrains.annotations.Nullable;

public final class QueryRenderContext {

    private final AstContext astContext;

    private final QueryAnalysis analysis;

    public QueryRenderContext(AstContext astContext, QueryAnalysis analysis) {
        this.astContext = astContext;
        this.analysis = analysis;
    }

    public AstContext getAstContext() {
        return astContext;
    }

    public QueryAnalysis getAnalysis() {
        return analysis;
    }

    @Nullable
    public BaseQueryExportSelection getBaseQueryExportSelection(BaseTableOwner baseTableOwner) {
        return analysis.getBaseQueryExportSelection(baseTableOwner);
    }

    @Nullable
    public BaseSelectionAliasRender getBaseSelectionRender(ConfigurableBaseQuery<?> query) {
        return analysis.getBaseSelectionRender(query);
    }

    @Nullable
    public JoinType getRequiredJoinType(TableImplementor<?> table) {
        return analysis.getRequiredJoinType(table);
    }
}
