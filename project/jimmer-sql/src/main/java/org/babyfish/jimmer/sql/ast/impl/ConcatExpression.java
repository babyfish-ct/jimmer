package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

class ConcatExpression
        extends AbstractExpression<String>
        implements StringExpressionImplementor {

    private final Expression<String> first;

    private final List<Expression<String>> others;

    ConcatExpression(Expression<String> first, List<Expression<String>> others) {
        this.first = first;
        this.others = others;
    }

    @Override
    public void accept(@NotNull AstVisitor visitor) {
        ((Ast) first).accept(visitor);
        for (Expression<?> other : others) {
            ((Ast) other).accept(visitor);
        }
    }

    @Override
    public void renderTo(@NotNull SqlBuilder builder) {
        builder.sql("concat(");
        renderChild((Ast) first, builder);
        for (Expression<?> other : others) {
            builder.sql(", ");
            renderChild((Ast) other, builder);
        }
        builder.sql(")");
    }

    @Override
    public int precedence() {
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConcatExpression that = (ConcatExpression) o;
        return first.equals(that.first) && others.equals(that.others);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, others);
    }
}
