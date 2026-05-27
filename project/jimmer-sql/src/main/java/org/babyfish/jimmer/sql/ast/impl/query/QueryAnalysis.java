package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.sql.ast.impl.Ast;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;

public final class QueryAnalysis {

    private final AstContext astContext;

    private QueryAnalysis(AstContext astContext) {
        this.astContext = astContext;
    }

    public static QueryAnalysis analyze(AstContext astContext, Ast ast) {
        UseTableVisitor visitor = new UseTableVisitor(astContext) {
            @Override
            public void visitStatement(AbstractMutableStatementImpl statement) {
                super.visitStatement(statement);
                BaseQueryExportAnalysis.analyze(statement, getAstContext());
            }
        };
        ast.accept(visitor);
        visitor.allocateAliases();
        return new QueryAnalysis(astContext);
    }

    public AstContext getAstContext() {
        return astContext;
    }
}
