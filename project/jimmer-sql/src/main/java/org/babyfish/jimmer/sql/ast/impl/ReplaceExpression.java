package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

class ReplaceExpression extends AbstractExpression<String> implements StringExpressionImplementor {

    private Expression<String> raw;
    private final String target;
    private final String replacement;

    ReplaceExpression(Expression<String> raw, String target, String replacement) {
        this.raw = raw;
        this.target = target;
        this.replacement = replacement;
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
        builder.sql("replace(");
        ((Ast)raw).renderTo(builder);
        builder.sql(", ");
        builder.rawVariable(target);
        builder.sql(", ");
        builder.rawVariable(replacement);
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
        ReplaceExpression that = (ReplaceExpression) o;
        return raw.equals(that.raw) && 
               target.equals(that.target) && 
               replacement.equals(that.replacement);
    }

    @Override
    public int hashCode() {
        return Objects.hash(raw, target, replacement);
    }
} 