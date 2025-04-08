package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.NumericExpression;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

class PositionExpression extends AbstractExpression<Integer> implements NumericExpressionImplementor<Integer> {

    private final String substring;
    private Expression<String> raw;

    PositionExpression(String substring, Expression<String> raw) {
        this.substring = substring;
        this.raw = raw;
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
        builder.sql("position(");
        builder.rawVariable(substring);
        builder.sql(" in ");
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PositionExpression that = (PositionExpression) o;
        return substring.equals(that.substring) && raw.equals(that.raw);
    }

    @Override
    public int hashCode() {
        return Objects.hash(substring, raw);
    }
} 