package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.StringExpression;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

class LPadExpression extends AbstractExpression<String> implements StringExpressionImplementor {

    private Expression<String> raw;
    private final Expression<Integer> length;
    private final String padString;

    LPadExpression(Expression<String> raw, Expression<Integer> length, String padString) {
        this.raw = raw;
        this.length = length;
        this.padString = padString;
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
        builder.sqlClient().getDialect().renderLPad(builder, (Ast) raw, (Ast) length, padString);
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
            return new LPadExpression(raw, resolvedLength, padString);
        }
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LPadExpression that = (LPadExpression) o;
        return raw.equals(that.raw) && 
               length.equals(that.length) && 
               padString.equals(that.padString);
    }

    @Override
    public int hashCode() {
        return Objects.hash(raw, length, padString);
    }
} 