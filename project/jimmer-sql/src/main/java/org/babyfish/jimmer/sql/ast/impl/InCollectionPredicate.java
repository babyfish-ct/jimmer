package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.render.ComparisonPredicates;
import org.babyfish.jimmer.sql.ast.impl.value.ValueGetter;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.*;

class InCollectionPredicate extends AbstractPredicate {

    private Expression<?> expression;

    private final Collection<?> values;

    private final boolean nullable;

    private final boolean negative;

    public InCollectionPredicate(
            Expression<?> expression,
            Collection<?> values,
            boolean nullable,
            boolean negative
    ) {
        this.expression = expression;
        this.values = values;
        this.nullable = nullable;
        this.negative = negative;
    }

    @Override
    public void accept(@NotNull AstVisitor visitor) {
        Ast.of(expression).accept(visitor);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void renderTo(@NotNull AbstractSqlBuilder<?> builder) {
        ComparisonPredicates.renderIn(
                nullable,
                negative,
                expression,
                values,
                builder.assertSimple()
        );
    }

    @Override
    public int precedence() {
        return 0;
    }

    @Override
    protected boolean determineHasVirtualPredicate() {
        return hasVirtualPredicate(expression);
    }

    @Override
    protected Ast onResolveVirtualPredicate(AstContext ctx) {
        this.expression = ctx.resolveVirtualPredicate(expression);
        return this;
    }

    @Override
    public Predicate not() {
        return new InCollectionPredicate(expression, values, nullable, !negative);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InCollectionPredicate)) return false;
        InCollectionPredicate that = (InCollectionPredicate) o;
        return negative == that.negative && expression.equals(that.expression) && values.equals(that.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expression, values, negative);
    }
}
