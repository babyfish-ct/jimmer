package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.runtime.SqlBuilder;

abstract class AbstractExpression<T> implements ExpressionImplementor<T>, Ast {

    private boolean isLowestPrecedenceUsing = false;

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
}
