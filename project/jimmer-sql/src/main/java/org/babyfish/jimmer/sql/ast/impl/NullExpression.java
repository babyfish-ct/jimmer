package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.NotNull;

class NullExpression<T> extends AbstractExpression<T> {

    private Class<T> type;

    public NullExpression(Class<T> type) {
        this.type = type;
    }

    @Override
    public void accept(@NotNull AstVisitor visitor) {}

    @Override
    public void renderTo(@NotNull SqlBuilder builder) {
        builder.sql("null");
    }

    @Override
    public Class<T> getType() {
        return type;
    }

    @Override
    public int precedence() {
        return 0;
    }
}
