package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.query.TypedSubQuery;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public abstract class SubQueryFunctionExpression<R> extends AbstractExpression<R> {

    private TypedSubQuery<R> subQuery;

    SubQueryFunctionExpression(TypedSubQuery<R> subQuery) {
        this.subQuery = subQuery;
    }

    protected abstract String functionName();

    @SuppressWarnings("unchecked")
    @Override
    public Class<R> getType() {
        return (Class<R>) ((ExpressionImplementor<?>) subQuery).getType();
    }

    @Override
    public int precedence() {
        return 0;
    }

    @Override
    public void accept(@NotNull AstVisitor visitor) {
        ((Ast) subQuery).accept(visitor);
    }

    @Override
    public void renderTo(@NotNull SqlBuilder builder) {
        builder.sql(functionName());
        renderChild((Ast) subQuery, builder);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SubQueryFunctionExpression<?> that = (SubQueryFunctionExpression<?>) o;
        return subQuery.equals(that.subQuery);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subQuery);
    }

    public static class All<R> extends SubQueryFunctionExpression<R> {

        public All(TypedSubQuery<R> subQuery) {
            super(subQuery);
        }

        @Override
        protected String functionName() {
            return "all";
        }
    }

    public static class Any<R> extends SubQueryFunctionExpression<R> {

        public Any(TypedSubQuery<R> subQuery) {
            super(subQuery);
        }

        @Override
        protected String functionName() {
            return "any";
        }
    }
}
