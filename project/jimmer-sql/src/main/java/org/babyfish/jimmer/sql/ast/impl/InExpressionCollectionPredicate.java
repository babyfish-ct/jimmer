package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.render.ComparisonPredicates;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class InExpressionCollectionPredicate extends AbstractPredicate {

    private final boolean negative;

    private Expression<?> expression;

    private Collection<Expression<?>> operands;

    public InExpressionCollectionPredicate(
            boolean negative, Expression<?> expression,
            Collection<Expression<?>> operands
    ) {
        this.negative = negative;
        this.expression = expression;
        this.operands = operands;
    }

    @Override
    protected boolean determineHasVirtualPredicate() {
        return hasVirtualPredicate(expression) || hasVirtualPredicate(operands);
    }

    @Override
    protected Ast onResolveVirtualPredicate(AstContext ctx) {
        this.expression = ctx.resolveVirtualPredicate(expression);
        List<Expression<?>> newOperands = new ArrayList<>(operands.size());
        for (Expression<?> operand : operands) {
            newOperands.add(ctx.resolveVirtualPredicate(operand));
        }
        this.operands = newOperands;
        return this;
    }

    @Override
    public void accept(@NotNull AstVisitor visitor) {
        ((Ast) expression).accept(visitor);
        for (Expression<?> operand : operands) {
            ((Ast)operand).accept(visitor);
        }
    }

    @Override
    public void renderTo(@NotNull AbstractSqlBuilder<?> builder) {
        ComparisonPredicates.renderExpressionIn(
                negative,
                expression,
                operands,
                builder
        );
    }

    @Override
    public int precedence() {
        return 0;
    }

    @Override
    public Predicate not() {
        return new InExpressionCollectionPredicate(!negative, expression, operands);
    }
}
