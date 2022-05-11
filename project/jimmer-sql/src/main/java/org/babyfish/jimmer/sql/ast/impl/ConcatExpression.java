package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

import java.util.List;

class ConcatExpression
        extends AbstractExpression<String>
        implements StringExpressionImplementor {

    private Expression<String> first;

    private List<Expression<String>> others;

    ConcatExpression(Expression<String> first, List<Expression<String>> others) {
        this.first = first;
        this.others = others;
    }

    @Override
    public void accept(AstVisitor visitor) {
        ((Ast) first).accept(visitor);
        for (Expression<?> other : others) {
            ((Ast) other).accept(visitor);
        }
    }

    @Override
    public void renderTo(SqlBuilder builder) {
        builder.sql("concat(");
        renderChild((Ast) first, builder);
        for (Expression<?> other : others) {
            builder.sql(", ");
            renderChild((Ast) other, builder);
        }
        builder.sql(")");
    }

    @Override
    public int precedence() {
        return 0;
    }
}
