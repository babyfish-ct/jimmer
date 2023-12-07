package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

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

    protected static <E extends Expression<?>> List<E> validateNoVirtualPredicate(List<E> expressions, Function<Integer, String> itemName) {
        int size = expressions.size();
        for (int i = 0; i < size; i++) {
            validateNoVirtualPredicate(expressions.get(i), itemName.apply(i));
        }
        return expressions;
    }

    protected static <E extends Expression<?>> E validateNoVirtualPredicate(E expression, String name) {
        if (expression == null) {
            throw new IllegalArgumentException(
                    "The argument \"" +
                            name +
                            "\" cannot be null"
            );
        }
        if (((Ast) expression).hasVirtualPredicate()) {
            throw new IllegalArgumentException(
                    "The argument \"" +
                            name +
                            "\" cannot has virtual predicate"
            );
        }
        return expression;
    }

    @Override
    public final boolean hasVirtualPredicate() {
        Boolean has = hasVirtualPredicate;
        if (has == null) {
            hasVirtualPredicate = has = determineHasVirtualPredicate();
        }
        return has;
    }

    @Override
    public final Ast resolveVirtualPredicate(AstContext ctx) {
        if (!hasVirtualPredicate()) {
            return this;
        }
        return onResolveVirtualPredicate(ctx);
    }

    protected Ast onResolveVirtualPredicate(AstContext ctx) {
        throw new UnsupportedOperationException(
                "`onResolveVirtualPredicate` is not overridden by \"" + getClass().getName() + "\""
        );
    }

    protected boolean determineHasVirtualPredicate() {
        return false;
    }

    protected static boolean hasVirtualPredicate(Object expression) {
        return ((Ast) expression).hasVirtualPredicate();
    }

    protected static boolean hasVirtualPredicate(Collection<?> expressions) {
        for (Object expression : expressions) {
            if (((Ast) expression).hasVirtualPredicate()) {
                return true;
            }
        }
        return false;
    }

    protected static <T> boolean hasVirtualPredicate(T[] expressions) {
        for (T expression : expressions) {
            if (((Ast) expression).hasVirtualPredicate()) {
                return true;
            }
        }
        return false;
    }
}
