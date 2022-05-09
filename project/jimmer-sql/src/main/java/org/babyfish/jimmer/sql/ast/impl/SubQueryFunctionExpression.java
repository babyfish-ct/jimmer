package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.query.TypedSubQuery;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

abstract class SubQueryFunctionExpression<R> extends AbstractExpression<R> {

    private TypedSubQuery<R> subQuery;

    public SubQueryFunctionExpression(TypedSubQuery<R> subQuery) {
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
    public void accept(AstVisitor visitor) {
        ((Ast) subQuery).accept(visitor);
    }

    @Override
    public void renderTo(SqlBuilder builder) {
        builder.sql(functionName());
        renderChild((Ast) subQuery, builder);
    }

    static class All<R> extends SubQueryFunctionExpression<R> {

        public All(TypedSubQuery<R> subQuery) {
            super(subQuery);
        }

        @Override
        protected String functionName() {
            return "all";
        }
    }

    static class Any<R> extends SubQueryFunctionExpression<R> {

        public Any(TypedSubQuery<R> subQuery) {
            super(subQuery);
        }

        @Override
        protected String functionName() {
            return "any";
        }
    }
}
