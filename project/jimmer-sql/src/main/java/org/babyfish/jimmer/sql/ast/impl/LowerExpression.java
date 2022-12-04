package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.StringExpression;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.NotNull;

class LowerExpression extends AbstractExpression<String> implements StringExpressionImplementor {

    private final Expression<String> raw;

    LowerExpression(Expression<String> raw) {
        this.raw = raw;
    }

    @Override
    public int precedence() {
        return 0;
    }

    @Override
    public void accept(@NotNull AstVisitor visitor) {
        ((Ast)raw).accept(visitor);
    }

    @Override
    public void renderTo(@NotNull SqlBuilder builder) {
        builder.sql("lower(");
        ((Ast)raw).renderTo(builder);
        builder.sql(")");
    }

    @Override
    public StringExpression lower() {
        return this;
    }
}
