package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.base.BaseQueryExportCollectorSelection;
import org.babyfish.jimmer.sql.ast.impl.base.BaseQueryExports;
import org.babyfish.jimmer.sql.ast.impl.base.BaseQueryExportsCollector;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableOwner;
import org.babyfish.jimmer.sql.ast.impl.table.TableAliases;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.table.Table;

import java.util.*;

final class QueryAnalysisBuilder implements TypedQueryImplementor.SelectionJoinRequirementCollector {

    private final AstContext astContext;

    private final QueryAnalysisContext analysisContext;

    private final BaseQueryExportsCollector baseQueryExportsCollector;

    private final JoinRequirements joinRequirements = new JoinRequirements();

    private final boolean materializeAliases;

    private BaseQueryExportUsages baseQueryExportUsages = BaseQueryExportUsages.EMPTY;

    private QueryAnalysisBuilder(AstContext astContext, boolean materializeAliases) {
        this.astContext = astContext;
        this.materializeAliases = materializeAliases;
        this.analysisContext = new QueryAnalysisContext(astContext);
        this.baseQueryExportsCollector = new BaseQueryExportsCollector(analysisContext);
    }

    static QueryAnalysis analyze(AstContext astContext, TypedQueryImplementor ast) {
        return analyze(astContext, ast, true);
    }

    static QueryAnalysis analyze(AstContext astContext, TypedQueryImplementor ast, boolean materializeAliases) {
        return new QueryAnalysisBuilder(astContext, materializeAliases).analyze(ast);
    }

    @Override
    public QueryAnalysisContext analysisContext() {
        return analysisContext;
    }

    QueryAnalysisContext getAnalysisContext() {
        return analysisContext;
    }

    BaseQueryExportCollectorSelection requireBaseQueryExportSelection(BaseTableOwner baseTableOwner) {
        return baseQueryExportsCollector.exportSelection(baseTableOwner);
    }

    private QueryAnalysis analyze(TypedQueryImplementor ast) {
        collectJoinRequirements(ast);
        QueryAnalysis joinAwareAnalysis = analysisFor(joinRequirements);
        TableUsageAnalysis tableUsageAnalysis = collectTableUsagesAndBaseExports(ast, joinAwareAnalysis);
        TableUsages tableUsages = tableUsageAnalysis.tableUsages;
        BaseQueryExports baseQueryExports = baseQueryExportsCollector.toExports();
        CteTableDependencies cteTableDependencies = CteTableDependencyAnalyzer.analyze(
                tableUsageAnalysis.statements,
                tableUsages,
                astContext
        );
        TableAliases tableAliases = materializeAliases ?
                prepareAliasBindings(tableUsages, cteTableDependencies) :
                TableAliases.EMPTY;
        QueryAnalysisModel model = new QueryAnalysisModel(
                joinRequirements,
                baseQueryExportUsages,
                tableUsages,
                tableAliases,
                baseQueryExports,
                cteTableDependencies
        );
        return new QueryAnalysis(astContext, model);
    }

    private QueryAnalysis analysisFor(JoinRequirements joinRequirements) {
        return new QueryAnalysis(
                astContext,
                new QueryAnalysisModel(
                        joinRequirements,
                        BaseQueryExportUsages.EMPTY,
                        TableUsages.EMPTY,
                        TableAliases.EMPTY,
                        BaseQueryExports.EMPTY,
                        CteTableDependencies.EMPTY
                )
        );
    }

    private void collectJoinRequirements(TypedQueryImplementor ast) {
        ast.collectSelectionJoinRequirements(this);
    }

    private TableUsageAnalysis collectTableUsagesAndBaseExports(TypedQueryImplementor ast, QueryAnalysis joinAwareAnalysis) {
        List<AbstractMutableStatementImpl> statements = new ArrayList<>();
        Set<AbstractMutableStatementImpl> statementSet = Collections.newSetFromMap(new IdentityHashMap<>());
        TableUsageCollector visitor = new TableUsageCollector(astContext, joinAwareAnalysis) {
            @Override
            public void visitStatement(AbstractMutableStatementImpl statement) {
                super.visitStatement(statement);
                if (statementSet.add(statement)) {
                    statements.add(statement);
                }
            }
        };
        ast.accept(visitor);
        baseQueryExportUsages = visitor.toBaseQueryExportUsages();
        for (AbstractMutableStatementImpl statement : statements) {
            analysisContext.pushStatement(statement);
            try {
                baseQueryExportsCollector.registerStatement(statement);
                BaseQueryExportAnalysis.analyze(statement, QueryAnalysisBuilder.this);
                BaseQueryExportAnalysis.analyzeUsages(QueryAnalysisBuilder.this);
            } finally {
                analysisContext.popStatement();
            }
        }
        return new TableUsageAnalysis(visitor.toTableUsages(), statements);
    }

    private TableAliases prepareAliasBindings(TableUsages tableUsages, CteTableDependencies cteTableDependencies) {
        tableUsages.applyTo(astContext);
        return tableUsages.allocateAndBindAliases(astContext, cteTableDependencies);
    }

    @Override
    public void require(Table<?> table) {
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

    boolean isExpressionExportRequired(BaseTableOwner baseTableOwner) {
        return baseQueryExportUsages.isExpressionExportRequired(baseTableOwner);
    }

    List<BaseQueryExportUsages.TableReferenceUsage> tableReferenceUsages(BaseTableOwner baseTableOwner) {
        return baseQueryExportUsages.tableReferenceUsages(baseTableOwner);
    }

    Set<BaseTableOwner> baseQueryExportOwners() {
        return baseQueryExportUsages.owners();
    }

    private static class TableUsageAnalysis {

        final TableUsages tableUsages;

        final List<AbstractMutableStatementImpl> statements;

        TableUsageAnalysis(TableUsages tableUsages, List<AbstractMutableStatementImpl> statements) {
            this.tableUsages = tableUsages;
            this.statements = statements;
        }
    }
}
