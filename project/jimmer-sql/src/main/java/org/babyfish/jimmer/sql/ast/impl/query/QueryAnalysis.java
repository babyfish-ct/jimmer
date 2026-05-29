package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.sql.ast.impl.Ast;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
import org.babyfish.jimmer.sql.ast.impl.base.BaseQueryExportPlan;
import org.babyfish.jimmer.sql.ast.impl.base.BaseQueryExportSelection;
import org.babyfish.jimmer.sql.ast.impl.base.BaseSelectionAliasRender;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableOwner;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ast.impl.table.RealTable;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.TableProxies;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.query.ConfigurableBaseQuery;
import org.jetbrains.annotations.Nullable;

public final class QueryAnalysis {

    private final AstContext astContext;

    private final BaseQueryExportPlan baseQueryExportPlan;

    private final JoinRequirementPlan joinRequirementPlan = new JoinRequirementPlan();

    private QueryAnalysis(AstContext astContext) {
        this.astContext = astContext;
        this.baseQueryExportPlan = new BaseQueryExportPlan(astContext);
    }

    public static QueryAnalysis analyze(AstContext astContext, Ast ast) {
        QueryAnalysis analysis = new QueryAnalysis(astContext);
        analysis.analyzeJoinRequirements(ast);
        UseTableVisitor visitor = new UseTableVisitor(astContext, analysis) {
            @Override
            public void visitStatement(AbstractMutableStatementImpl statement) {
                super.visitStatement(statement);
                BaseQueryExportAnalysis.analyze(statement, analysis);
            }

            @Override
            public void visitTableReference(RealTable table, @Nullable ImmutableProp prop, boolean rawId) {
                super.visitTableReference(table, prop, rawId);
                BaseQueryExportAnalysis.analyzeTableReference(table, prop, rawId, analysis);
            }

            @Override
            public void visitBaseTableExpression(BaseTableOwner baseTableOwner) {
                analysis.getBaseQueryExportSelection(baseTableOwner).expressionIndex();
            }
        };
        ast.accept(visitor);
        visitor.allocateAliases();
        analysis.baseQueryExportPlan.freeze();
        return analysis;
    }

    private void analyzeJoinRequirements(Ast ast) {
        if (!(ast instanceof AbstractConfigurableTypedQueryImpl)) {
            return;
        }
        AbstractConfigurableTypedQueryImpl query = (AbstractConfigurableTypedQueryImpl) ast;
        astContext.pushStatement(query.getMutableQuery());
        try {
            for (Selection<?> selection : query.getSelections()) {
                if (selection instanceof Table<?>) {
                    analyzeSelectionJoinRequirement(astContext, (Table<?>) selection);
                }
            }
        } finally {
            astContext.popStatement();
        }
    }

    private void analyzeSelectionJoinRequirement(AstContext astContext, Table<?> table) {
        TableImplementor<?> tableImplementor = TableProxies.resolve(table, astContext);
        if (tableImplementor.getBaseTableOwner() == null) {
            return;
        }
        ImmutableProp joinProp = tableImplementor.getJoinProp();
        if (joinProp != null && joinProp.isNullable()) {
            joinRequirementPlan.require(tableImplementor, JoinType.LEFT);
        }
    }

    public AstContext getAstContext() {
        return astContext;
    }

    @Nullable
    public BaseQueryExportSelection getBaseQueryExportSelection(BaseTableOwner baseTableOwner) {
        return baseQueryExportPlan.exportSelection(baseTableOwner);
    }

    @Nullable
    public BaseSelectionAliasRender getBaseSelectionRender(ConfigurableBaseQuery<?> query) {
        return baseQueryExportPlan.baseSelectionRender(query);
    }

    @Nullable
    public JoinType getRequiredJoinType(TableImplementor<?> table) {
        return joinRequirementPlan.get(table);
    }
}
