package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
import org.babyfish.jimmer.sql.ast.impl.Ast;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.base.BaseQueryExportCollectorSelection;
import org.babyfish.jimmer.sql.ast.impl.base.BaseQueryExports;
import org.babyfish.jimmer.sql.ast.impl.base.BaseQueryExportsCollector;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableOwner;
import org.babyfish.jimmer.sql.ast.impl.table.TableAliases;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.table.Table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

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
        collectJoinRequirements(ast);
        QueryAnalysis joinAwareAnalysis = analysisFor(
                joinRequirements,
                BaseQueryExportUsages.EMPTY,
                TableUsages.EMPTY,
                BaseQueryExports.EMPTY
        );
        TableUsages tableUsages = collectTableUsagesAndBaseExports(ast, joinAwareAnalysis);
        BaseQueryExports baseQueryExports = baseQueryExportsCollector.toExports();
        TableAliases tableAliases = materialize(tableUsages);
        QueryAnalysisModel model = new QueryAnalysisModel(
                joinRequirements,
                baseQueryExportUsages,
                tableUsages,
                tableAliases,
                baseQueryExports
        );
        return new QueryAnalysis(astContext, model);
    }

    private QueryAnalysis analysisFor(
            JoinRequirements joinRequirements,
            BaseQueryExportUsages baseQueryExportUsages,
            TableUsages tableUsages,
            BaseQueryExports baseQueryExports
    ) {
        return new QueryAnalysis(
                astContext,
                new QueryAnalysisModel(
                        joinRequirements,
                        baseQueryExportUsages,
                        tableUsages,
                        TableAliases.EMPTY,
                        baseQueryExports
                )
        );
    }

    private void collectJoinRequirements(Ast ast) {
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

    private TableUsages collectTableUsagesAndBaseExports(Ast ast, QueryAnalysis joinAwareAnalysis) {
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
        return visitor.toTableUsages();
    }

    private TableAliases materialize(TableUsages tableUsages) {
        tableUsages.applyTo(astContext);
        return tableUsages.allocateAliases();
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

    boolean isExpressionExportRequired(BaseTableOwner baseTableOwner) {
        return baseQueryExportUsages.isExpressionExportRequired(baseTableOwner);
    }

    List<BaseQueryExportUsages.TableReferenceUsage> tableReferenceUsages(BaseTableOwner baseTableOwner) {
        return baseQueryExportUsages.tableReferenceUsages(baseTableOwner);
    }

    Set<BaseTableOwner> baseQueryExportOwners() {
        return baseQueryExportUsages.owners();
    }
}
