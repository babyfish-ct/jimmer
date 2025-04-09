package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.StringExpression;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

class SubstringExpression extends AbstractExpression<String> implements StringExpressionImplementor {

    private Expression<String> raw;
    private Expression<Integer> start;
    @Nullable
    private Expression<Integer> length;

    SubstringExpression(Expression<String> raw, Expression<Integer> start) {
        this.raw = raw;
        this.start = start;
        this.length = null;
    }

    SubstringExpression(Expression<String> raw, Expression<Integer> start, Expression<Integer> length) {
        this.raw = raw;
        this.start = start;
        this.length = length;
    }

    @Override
    public int precedence() {
        return 0;
    }

    @Override
    public void accept(@NotNull AstVisitor visitor) {
        ((Ast)raw).accept(visitor);
        ((Ast)start).accept(visitor);
        if (length != null) {
            ((Ast)length).accept(visitor);
        }
    }

    @Override
    public void renderTo(@NotNull AbstractSqlBuilder<?> builder) {
        // Most databases use "SUBSTRING" function name
        builder.sql("substring(");
        ((Ast)raw).renderTo(builder);
        builder.sql(", ");
        ((Ast)start).renderTo(builder);
        if (length != null) {
            builder.sql(", ");
            ((Ast)length).renderTo(builder);
        }
        builder.sql(")");
    }

    @Override
    protected boolean determineHasVirtualPredicate() {
        return hasVirtualPredicate(raw) || 
               hasVirtualPredicate(start) || 
               (length != null && hasVirtualPredicate(length));
    }

    @Override
    protected Ast onResolveVirtualPredicate(AstContext ctx) {
        raw = ctx.resolveVirtualPredicate(raw);
        start = ctx.resolveVirtualPredicate(start);
        if (length != null) {
            length = ctx.resolveVirtualPredicate(length);
        }
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SubstringExpression that = (SubstringExpression) o;
        return raw.equals(that.raw) && 
               start.equals(that.start) && 
               Objects.equals(length, that.length);
    }

    @Override
    public int hashCode() {
        return Objects.hash(raw, start, length);
    }
} 