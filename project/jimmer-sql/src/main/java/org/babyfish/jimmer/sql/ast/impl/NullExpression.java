package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class NullExpression<T> extends AbstractExpression<T> {

    private final Class<T> type;

    public NullExpression(Class<T> type) {
        this.type = type;
    }

    @Override
    public void accept(@NotNull AstVisitor visitor) {}

    @Override
    public void renderTo(@NotNull AbstractSqlBuilder<?> builder) {
        builder.sql("null");
    }

    @Override
    protected boolean determineHasVirtualPredicate() {
        return false;
    }

    @Override
    protected Ast onResolveVirtualPredicate(AstContext ctx) {
        return this;
    }

    @Override
    public Class<T> getType() {
        return type;
    }

    @Override
    public int precedence() {
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NullExpression<?> that = (NullExpression<?>) o;
        return type.equals(that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type);
    }
}
