package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableOwner;
import org.babyfish.jimmer.sql.ast.impl.query.QueryAnalysis;
import org.babyfish.jimmer.sql.ast.impl.query.QueryRenderContext;
import org.babyfish.jimmer.sql.ast.impl.table.RealTable;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.TableLikeImplementor;
import org.babyfish.jimmer.sql.ast.query.TypedSubQuery;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.Field;
import org.jetbrains.annotations.Nullable;

public abstract class AstVisitor {

    private final AstContext ctx;

    private final QueryRenderContext queryRenderContext;

    public AstVisitor(AstContext ctx) {
        this(ctx, null);
    }

    public AstVisitor(AstContext ctx, @Nullable QueryAnalysis queryAnalysis) {
        this.ctx = ctx;
        this.queryRenderContext = queryAnalysis != null ? new QueryRenderContext(ctx, queryAnalysis) : null;
    }

    public AstContext getAstContext() {
        return ctx;
    }

    @Nullable
    public QueryRenderContext getQueryRenderContext() {
        return queryRenderContext;
    }

    public void visitTableReference(RealTable table, @Nullable ImmutableProp prop, boolean rawId) {}

    public final RealTable realTableForAnalysis(TableLikeImplementor<?> tableLikeImplementor) {
        return queryRenderContext != null ?
                tableLikeImplementor.realTableForAnalysis(queryRenderContext) :
                tableLikeImplementor.realTable(ctx);
    }

    public void visitTableFetcherField(RealTable table, Field field) {
        visitTableReference(table, field.getProp(), field.isRawId());
    }

    public void visitTableFetcher(RealTable table, Fetcher<?> fetcher) {}

    public void visitBaseTableExpression(BaseTableOwner baseTableOwner) {}

    public void visitStatement(AbstractMutableStatementImpl statement) {}

    public boolean visitSubQuery(TypedSubQuery<?> subQuery) {
        return true;
    }

    public void visitAggregation(String functionName, Expression<?> expression, String prefix) {
        ((Ast) expression).accept(this);
    }
}
