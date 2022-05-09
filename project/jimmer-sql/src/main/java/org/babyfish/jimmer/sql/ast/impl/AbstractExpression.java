package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.query.TypedSubQuery;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

import java.util.Collection;

abstract class AbstractExpression<T> implements ExpressionImplementor<T>, Ast {

    protected void renderChild(Ast ast, SqlBuilder builder) {
        if (!(ast instanceof ExpressionImplementor<?>) ||
                ((ExpressionImplementor<?>)ast).precedence() <= precedence()) {
            ast.renderTo(builder);
        } else {
            builder.sql("(");
            ast.renderTo(builder);
            builder.sql(")");
        }
    }
}
