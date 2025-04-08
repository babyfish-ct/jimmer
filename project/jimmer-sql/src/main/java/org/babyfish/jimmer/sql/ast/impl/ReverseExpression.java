package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.StringExpression;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

class ReverseExpression extends AbstractExpression<String> implements StringExpressionImplementor {

    private Expression<String> raw;

    ReverseExpression(Expression<String> raw) {
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
    public void renderTo(@NotNull AbstractSqlBuilder<?> builder) {
        builder.sql("reverse(");
        ((Ast)raw).renderTo(builder);
        builder.sql(")");
    }

    @Override
    protected boolean determineHasVirtualPredicate() {
        return hasVirtualPredicate(raw);
    }

    @Override
    protected Ast onResolveVirtualPredicate(AstContext ctx) {
        raw = ctx.resolveVirtualPredicate(raw);
        return this;
    }

    @Override
    public StringExpression reverse() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReverseExpression that = (ReverseExpression) o;
        return raw.equals(that.raw);
    }

    @Override
    public int hashCode() {
        return Objects.hash(raw);
    }
} 