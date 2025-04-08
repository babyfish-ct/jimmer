package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.NumericExpression;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

class LocateExpression extends AbstractExpression<Integer> implements NumericExpressionImplementor<Integer> {

    private final String substring;
    private Expression<String> raw;
    private final Expression<Integer> startPosition;

    LocateExpression(String substring, Expression<String> raw, Expression<Integer> startPosition) {
        this.substring = substring;
        this.raw = raw;
        this.startPosition = startPosition;
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
        if (startPosition != null) {
            ((Ast)startPosition).accept(visitor);
        }
    }

    @Override
    public void renderTo(@NotNull AbstractSqlBuilder<?> builder) {
        builder.sql("locate(");
        builder.rawVariable(substring);
        builder.sql(", ");
        ((Ast)raw).renderTo(builder);
        if (startPosition != null) {
            builder.sql(", ");
            ((Ast)startPosition).renderTo(builder);
        }
        builder.sql(")");
    }

    @Override
    protected boolean determineHasVirtualPredicate() {
        return hasVirtualPredicate(raw) || 
               (startPosition != null && hasVirtualPredicate(startPosition));
    }

    @Override
    protected Ast onResolveVirtualPredicate(AstContext ctx) {
        raw = ctx.resolveVirtualPredicate(raw);
        if (startPosition != null) {
            Expression<Integer> resolvedStart = ctx.resolveVirtualPredicate(startPosition);
            if (resolvedStart != startPosition) {
                return new LocateExpression(substring, raw, resolvedStart);
            }
        }
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LocateExpression that = (LocateExpression) o;
        return substring.equals(that.substring) && 
               raw.equals(that.raw) && 
               Objects.equals(startPosition, that.startPosition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(substring, raw, startPosition);
    }
} 