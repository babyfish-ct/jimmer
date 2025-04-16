package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.NumericExpression;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.jetbrains.annotations.NotNull;

public class UnaryMinusExpression<N extends Number & Comparable<N>>
        extends AbstractExpression<N>
        implements NumericExpressionImplementor<N> {

    private NumericExpression<N> expression;

    private UnaryMinusExpression(NumericExpression<N> expression) {
        this.expression = expression;
    }

    public static <N extends Number & Comparable<N>> NumericExpression<N> of(NumericExpression<N> expr) {
        if (expr instanceof UnaryMinusExpression<?>) {
            return ((UnaryMinusExpression<N>)expr).expression;
        }
        return new UnaryMinusExpression<>(expr);
    }

    @Override
    protected boolean determineHasVirtualPredicate() {
        return hasVirtualPredicate(expression);
    }

    @Override
    protected Ast onResolveVirtualPredicate(AstContext ctx) {
        expression = ctx.resolveVirtualPredicate(expression);
        return this;
    }

    @Override
    public void accept(@NotNull AstVisitor visitor) {
        ((Ast)expression).accept(visitor);
    }

    @Override
    public void renderTo(@NotNull AbstractSqlBuilder<?> builder) {
        builder.sql("-");
        renderChild((Ast) expression, builder);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<N> getType() {
        return ((ExpressionImplementor<N>)expression).getType();
    }

    @Override
    public int precedence() {
        return 0;
    }
}
