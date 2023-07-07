package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

class BetweenPredicate extends AbstractPredicate {

    private final boolean negative;

    private final Expression<?> expression;

    private final Expression<?> min;

    private final Expression<?> max;

    public BetweenPredicate(
            boolean negative,
            Expression<?> expression,
            Expression<?> min,
            Expression<?> max
    ) {
        this.negative = negative;
        this.expression = expression;
        this.min = min;
        this.max = max;
        Literals.bind(min, expression);
        Literals.bind(min, expression);
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
        builder.sql(negative ? " not between " : " between ");
        renderChild((Ast) min, builder);
        builder.sql(" and ");
        renderChild((Ast) max, builder);
    }

    @Override
    public Predicate not() {
        return new BetweenPredicate(!negative, expression, min, max);
    }

    @Override
    public int hashCode() {
        return Objects.hash(negative, expression, min, max);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BetweenPredicate)) return false;
        BetweenPredicate that = (BetweenPredicate) o;
        return negative == that.negative && expression.equals(that.expression) && Objects.equals(min, that.min) && Objects.equals(max, that.max);
    }
}
