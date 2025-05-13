package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

class PositionExpression extends AbstractExpression<Integer> implements NumericExpressionImplementor<Integer> {

    private Expression<String> subStr;

    private Expression<String> raw;

    @Nullable
    private Expression<Integer> start;

    @SuppressWarnings("unchecked")
    PositionExpression(
            Expression<String> subStr,
            Expression<String> raw,
            @Nullable Expression<Integer> start
    ) {
        if (start instanceof LiteralExpressionImplementor<?> &&
                ((LiteralExpressionImplementor<Integer>)start).getValue().equals(1)) {
            start = null;
        }
        this.subStr = subStr;
        this.raw = raw;
        this.start = start;
    }

    @Override
    public Class<Integer> getType() {
        return Integer.class;
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
        builder.sqlClient().getDialect().renderPosition(
                builder,
                precedence(),
                (Ast) subStr,
                (Ast) raw,
                (Ast) start
        );
    }

    @Override
    protected boolean determineHasVirtualPredicate() {
        return hasVirtualPredicate(raw);
    }

    @Override
    protected Ast onResolveVirtualPredicate(AstContext ctx) {
        subStr = ctx.resolveVirtualPredicate(subStr);
        raw = ctx.resolveVirtualPredicate(raw);
        start = ctx.resolveVirtualPredicate(start);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PositionExpression that = (PositionExpression) o;
        return subStr.equals(that.subStr) && raw.equals(that.raw);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subStr, raw);
    }
} 