package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.impl.table.RealTable;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.query.TypedSubQuery;
import org.jetbrains.annotations.Nullable;

public abstract class AstVisitor {

    private final AstContext ctx;

    public AstVisitor(AstContext ctx) {
        this.ctx = ctx;
    }

    public AstContext getAstContext() {
        return ctx;
    }

    public void visitTableReference(RealTable table, @Nullable ImmutableProp prop, boolean rawId) {}

    public void visitStatement(AbstractMutableStatementImpl statement) {}

    public boolean visitSubQuery(TypedSubQuery<?> subQuery) {
        return true;
    }

    public void visitAggregation(String functionName, Expression<?> expression, String prefix) {
        ((Ast) expression).accept(this);
    }
}
