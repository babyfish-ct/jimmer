package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.JoinType;
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
    public JoinType getRequiredJoinType(TableImplementor<?> table) {
        return analysis.getRequiredJoinType(table);
    }
}
