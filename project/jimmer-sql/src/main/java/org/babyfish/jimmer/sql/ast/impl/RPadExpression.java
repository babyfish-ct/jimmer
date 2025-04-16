package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.StringExpression;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

class RPadExpression extends AbstractExpression<String> implements StringExpressionImplementor {

    private Expression<String> raw;
    private Expression<Integer> length;
    private Expression<String> pad;

    RPadExpression(
            Expression<String> raw,
            Expression<Integer> length,
            Expression<String> pad
    ) {
        this.raw = raw;
        this.length = length;
        this.pad = pad;
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
        builder.sqlClient().getDialect().renderRPad(
                builder,
                precedence(),
                (Ast) raw,
                (Ast) length,
                (Ast) pad
        );
    }

    @Override
    protected boolean determineHasVirtualPredicate() {
        return hasVirtualPredicate(raw) ||
                hasVirtualPredicate(length) ||
                hasVirtualPredicate(pad);
    }

    @Override
    protected Ast onResolveVirtualPredicate(AstContext ctx) {
        raw = ctx.resolveVirtualPredicate(raw);
        length = ctx.resolveVirtualPredicate(length);
        pad = ctx.resolveVirtualPredicate(pad);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RPadExpression that = (RPadExpression) o;
        return raw.equals(that.raw) && 
               length.equals(that.length) && 
               pad.equals(that.pad);
    }

    @Override
    public int hashCode() {
        return Objects.hash(raw, length, pad);
    }
} 