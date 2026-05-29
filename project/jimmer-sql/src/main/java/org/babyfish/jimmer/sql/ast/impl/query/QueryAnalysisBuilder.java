package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
import org.babyfish.jimmer.sql.ast.impl.Ast;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.base.BaseQueryExportCollectorSelection;
import org.babyfish.jimmer.sql.ast.impl.base.BaseQueryExportsCollector;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableOwner;
import org.babyfish.jimmer.sql.ast.impl.table.RealTable;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.jetbrains.annotations.Nullable;

final class QueryAnalysisBuilder {

    private final AstContext astContext;

    private final QueryAnalysisContext analysisContext;

    private final BaseQueryExportsCollector baseQueryExportsCollector;

    private final JoinRequirements joinRequirements = new JoinRequirements();

    private BaseQueryExportUsages baseQueryExportUsages = BaseQueryExportUsages.EMPTY;

    private QueryAnalysisBuilder(AstContext astContext) {
        this.astContext = astContext;
        this.analysisContext = new QueryAnalysisContext(astContext);
        this.baseQueryExportsCollector = new BaseQueryExportsCollector(analysisContext);
    }

    static QueryAnalysis analyze(AstContext astContext, Ast ast) {
        return new QueryAnalysisBuilder(astContext).analyze(ast);
    }

    QueryAnalysisContext getAnalysisContext() {
        return analysisContext;
    }

    BaseQueryExportCollectorSelection requireBaseQueryExportSelection(BaseTableOwner baseTableOwner) {
        return baseQueryExportsCollector.exportSelection(baseTableOwner);
    }

    private QueryAnalysis analyze(Ast ast) {
        analyzeJoinRequirements(ast);
        QueryAnalysis joinAwareAnalysis = new QueryAnalysis(
                astContext,
                baseQueryExportsCollector.toExports(),
                joinRequirements
        );
        baseQueryExportUsages = BaseQueryExportUsageCollector.collect(astContext, ast, joinAwareAnalysis);
        TableUsageCollector visitor = new TableUsageCollector(astContext, joinAwareAnalysis) {
            @Override
            public void visitStatement(AbstractMutableStatementImpl statement) {
                super.visitStatement(statement);
                baseQueryExportsCollector.registerStatement(statement);
                BaseQueryExportAnalysis.analyze(statement, QueryAnalysisBuilder.this);
            }

            @Override
            public void visitTableReference(RealTable table, @Nullable ImmutableProp prop, boolean rawId) {
                super.visitTableReference(table, prop, rawId);
                BaseQueryExportAnalysis.analyzeTableReference(
                        table,
                        prop,
                        rawId,
                        QueryAnalysisBuilder.this
                );
            }

            @Override
            public void visitBaseTableExpression(BaseTableOwner baseTableOwner) {
                requireBaseQueryExportSelection(baseTableOwner).requireExpressionIndex();
            }
        };
        ast.accept(visitor);
        TableUsages tableUsages = visitor.toTableUsages();
        tableUsages.applyTo(astContext);
        tableUsages.allocateAliases();
        return new QueryAnalysis(
                astContext,
                baseQueryExportsCollector.toExports(),
                joinRequirements
        );
    }

    private void analyzeJoinRequirements(Ast ast) {
        if (!(ast instanceof AbstractConfigurableTypedQueryImpl)) {
            return;
        }
        AbstractConfigurableTypedQueryImpl query = (AbstractConfigurableTypedQueryImpl) ast;
        analysisContext.pushStatement(query.getMutableQuery());
        try {
            for (Selection<?> selection : query.getSelections()) {
                if (selection instanceof Table<?>) {
                    analyzeSelectionJoinRequirement((Table<?>) selection);
                }
            }
        } finally {
            analysisContext.popStatement();
        }
    }

    private void analyzeSelectionJoinRequirement(Table<?> table) {
        TableImplementor<?> tableImplementor = analysisContext.resolve(table);
        if (tableImplementor.getBaseTableOwner() == null) {
            return;
        }
        ImmutableProp joinProp = tableImplementor.getJoinProp();
        if (joinProp != null && joinProp.isNullable()) {
            joinRequirements.require(tableImplementor, JoinType.LEFT);
        }
    }

    boolean isFullRowExportRequired(BaseTableOwner baseTableOwner) {
        return baseQueryExportUsages.isFullRowExportRequired(baseTableOwner);
    }
}
