package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.StringExpression;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

class LeftExpression extends AbstractExpression<String> implements StringExpressionImplementor {

    private Expression<String> raw;
    private final Expression<Integer> length;

    LeftExpression(Expression<String> raw, Expression<Integer> length) {
        this.raw = raw;
        this.length = length;
    }

    @Override
    public int precedence() {
        return 0;
    }

    @Override
    public void accept(@NotNull AstVisitor visitor) {
        ((Ast)raw).accept(visitor);
        ((Ast)length).accept(visitor);
    }

    @Override
    public void renderTo(@NotNull AbstractSqlBuilder<?> builder) {
        builder.sql("left(");
        ((Ast)raw).renderTo(builder);
        builder.sql(", ");
        ((Ast)length).renderTo(builder);
        builder.sql(")");
    }

    @Override
    protected boolean determineHasVirtualPredicate() {
        return hasVirtualPredicate(raw) || hasVirtualPredicate(length);
    }

    @Override
    protected Ast onResolveVirtualPredicate(AstContext ctx) {
        raw = ctx.resolveVirtualPredicate(raw);
        Expression<Integer> resolvedLength = ctx.resolveVirtualPredicate(length);
        if (resolvedLength != length) {
            return new LeftExpression(raw, resolvedLength);
        }
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LeftExpression that = (LeftExpression) o;
        return raw.equals(that.raw) && length.equals(that.length);
    }

    @Override
    public int hashCode() {
        return Objects.hash(raw, length);
    }
} 