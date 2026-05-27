package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.sql.ast.impl.Ast;
import org.babyfish.jimmer.sql.ast.impl.AstContext;

public final class QueryAnalysis {

    private final AstContext astContext;

    private QueryAnalysis(AstContext astContext) {
        this.astContext = astContext;
    }

    public static QueryAnalysis analyze(AstContext astContext, Ast ast) {
        UseTableVisitor visitor = new UseTableVisitor(astContext);
        ast.accept(visitor);
        visitor.allocateAliases();
        return new QueryAnalysis(astContext);
    }

    public AstContext getAstContext() {
        return astContext;
    }
}
