package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.query.TypedSubQuery;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

class InSubQueryPredicate extends AbstractPredicate {

    private final Expression<?> expression;

    private final TypedSubQuery<?> subQuery;

    private final boolean negative;

    public InSubQueryPredicate(
            Expression<?> expression,
            TypedSubQuery<?> subQuery,
            boolean negative
    ) {
        this.expression = expression;
        this.subQuery = subQuery;
        this.negative = negative;
    }

    @Override
    public int precedence() {
        return 0;
    }

    @Override
    public Predicate not() {
        return new InSubQueryPredicate(
                expression,
                subQuery,
                !negative
        );
    }

    @Override
    public void accept(@NotNull AstVisitor visitor) {
        ((Ast) expression).accept(visitor);
        ((Ast) subQuery).accept(visitor);
    }

    @Override
    public void renderTo(@NotNull SqlBuilder builder) {
        renderChild((Ast) expression, builder);
        builder.sql(negative ? " not in " : " in ");
        renderChild((Ast) subQuery, builder);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InSubQueryPredicate)) return false;
        InSubQueryPredicate that = (InSubQueryPredicate) o;
        return negative == that.negative && expression.equals(that.expression) && subQuery.equals(that.subQuery);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expression, subQuery, negative);
    }
}
