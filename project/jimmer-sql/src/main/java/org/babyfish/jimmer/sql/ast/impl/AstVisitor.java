package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.query.TypedSubQuery;

public abstract class AstVisitor {

    private final AstContext ctx;

    public AstVisitor(AstContext ctx) {
        this.ctx = ctx;
    }

    public AstContext getAstContext() {
        return ctx;
    }

    public void visitTableReference(TableImplementor<?> table, ImmutableProp prop) {}

    public boolean visitSubQuery(TypedSubQuery<?> subQuery) {
        return true;
    }

    public void visitAggregation(String functionName, Expression<?> expression, String prefix) {
        ((Ast) expression).accept(this);
    }
}
