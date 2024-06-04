package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.impl.query.MutableStatementImplementor;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

import java.util.Collection;

public abstract class AbstractExpression<T> implements ExpressionImplementor<T>, Ast {

    private boolean isLowestPrecedenceUsing = false;

    private Boolean hasVirtualPredicate;

    protected void renderChild(Ast ast, SqlBuilder builder) {
        if (isLowestPrecedenceUsing || !(
                ast instanceof ExpressionImplementor<?>) ||
                ((ExpressionImplementor<?>)ast).precedence() <= precedence()) {
            ast.renderTo(builder);
        } else {
            builder.sql("(").space('\n');
            ast.renderTo(builder);
            builder.space('\n').sql(")");
        }
    }

    protected void usingLowestPrecedence(Runnable block) {
        if (isLowestPrecedenceUsing) {
            block.run();
        } else {
            isLowestPrecedenceUsing = true;
            try {
                block.run();
            } finally {
                isLowestPrecedenceUsing = false;
            }
        }
    }

    @Override
    public final boolean hasVirtualPredicate() {
        Boolean has = hasVirtualPredicate;
        if (has == null) {
            hasVirtualPredicate = has = determineHasVirtualPredicate();
        }
        return has;
    }

    protected abstract boolean determineHasVirtualPredicate();

    @Override
    public final Ast resolveVirtualPredicate(AstContext ctx) {
        if (!hasVirtualPredicate()) {
            return this;
        }
        return onResolveVirtualPredicate(ctx);
    }

    protected abstract Ast onResolveVirtualPredicate(AstContext ctx);

    protected static boolean hasVirtualPredicate(Object expression) {
        if (expression instanceof Ast && ((Ast) expression).hasVirtualPredicate()) {
            return true;
        }
        return expression instanceof MutableStatementImplementor &&
                ((MutableStatementImplementor) expression).hasVirtualPredicate();
    }

    protected static boolean hasVirtualPredicate(Collection<?> expressions) {
        for (Object expression : expressions) {
            if (hasVirtualPredicate(expression)) {
                return true;
            }
        }
        return false;
    }

    protected static <T> boolean hasVirtualPredicate(T[] expressions) {
        for (T expression : expressions) {
            if (hasVirtualPredicate(expression)) {
                return true;
            }
        }
        return false;
    }
}
