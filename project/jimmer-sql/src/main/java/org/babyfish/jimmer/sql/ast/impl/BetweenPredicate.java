package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.NotNull;

class BetweenPredicate extends AbstractPredicate {

    private Expression<?> expression;

    private Expression<?> min;

    private Expression<?> max;

    public BetweenPredicate(Expression<?> expression, Expression<?> min, Expression<?> max) {
        this.expression = expression;
        this.min = min;
        this.max = max;
    }

    @Override
    public int precedence() {
        return 7;
    }

    @Override
    public void accept(@NotNull AstVisitor visitor) {
        ((Ast) expression).accept(visitor);
        ((Ast) min).accept(visitor);
        ((Ast) max).accept(visitor);
    }

    @Override
    public void renderTo(@NotNull SqlBuilder builder) {
        renderChild((Ast) expression, builder);
        builder.sql(" between ");
        renderChild((Ast) min, builder);
        builder.sql(" and ");
        renderChild((Ast) max, builder);
    }
}
