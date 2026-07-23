package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.base.BaseQueryExportSelection;
import org.babyfish.jimmer.sql.ast.impl.base.BaseQueryReadSupport;
import org.babyfish.jimmer.sql.ast.impl.base.BaseSelectionAliasRender;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableOwner;
import org.babyfish.jimmer.sql.ast.impl.table.RealTable;
import org.babyfish.jimmer.sql.ast.impl.table.TableAliasScope;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.query.ConfigurableBaseQuery;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

public final class QueryRenderContext {

    private final AstContext astContext;

    private final QueryAnalysis analysis;

    private final BaseQueryReadSupport baseQueryReadSupport;

    private QueryRenderContext(AstContext astContext, QueryAnalysis analysis) {
        this.astContext = astContext;
        this.analysis = analysis;
        this.baseQueryReadSupport = new BaseQueryReadSupport(this);
    }

    public static QueryRenderContext forAnalysis(AstContext astContext, QueryAnalysis analysis) {
        return new QueryRenderContext(astContext, analysis);
    }

    public static QueryRenderContext forRender(AstContext astContext, QueryAnalysis analysis) {
        analysis.applyTo(astContext);
        return new QueryRenderContext(astContext, analysis);
    }

    public AstContext getAstContext() {
        return astContext;
    }

    public BaseQueryReadSupport getBaseQueryReadSupport() {
        return baseQueryReadSupport;
    }

    public QueryAnalysis getAnalysis() {
        return analysis;
    }

    public TableAliasScope getTableAliasScope() {
        return astContext.getTableAliasScope();
    }

    @Nullable
    public BaseSelectionAliasRender getBaseSelectionRender(ConfigurableBaseQuery<?> query) {
        return analysis.getBaseSelectionRender(query);
    }

    @Nullable
    public JoinType getRequiredJoinType(TableImplementor<?> table) {
        return analysis.getRequiredJoinType(table);
    }

    public boolean isJoinedTypeBranchTableRequired(TableImplementor<?> table, ImmutableType stageType) {
        return analysis.isJoinedTypeBranchTableRequired(table, stageType);
    }

    public Set<ImmutableType> getJoinedTypeBranchTableTypes(TableImplementor<?> table) {
        return analysis.getJoinedTypeBranchTableTypes(table);
    }

    @Nullable
    public BaseQueryExportSelection getBaseQueryExportSelection(BaseTableOwner baseTableOwner) {
        return analysis.getBaseQueryExportSelection(baseTableOwner);
    }

    List<CteTableDeclaration> getCteTableDeclarations(AbstractMutableStatementImpl statement) {
        return analysis.getCteTableDeclarations(statement);
    }

    public void applyAliases(RealTable table) {
        TableAliasScope aliasScope = getTableAliasScope();
        if (aliasScope != null) {
            aliasScope.applyAliases(table, analysis.getTableAliases());
        }
    }
}
