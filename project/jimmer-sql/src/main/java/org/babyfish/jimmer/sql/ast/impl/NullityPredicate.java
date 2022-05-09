package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

public class NullityPredicate extends AbstractPredicate {

    private Expression<?> expression;

    private boolean negative;

    public NullityPredicate(Expression<?> expression, boolean negative) {
        this.expression = expression;
        this.negative = negative;
    }

    @Override
    public void accept(AstVisitor visitor) {
        ((Ast) expression).accept(visitor);
    }

    @Override
    public void renderTo(SqlBuilder builder) {
        renderChild((Ast) expression, builder);
        if (negative) {
            builder.sql(" is not null");
        } else {
            builder.sql(" is null");
        }
    }

    @Override
    public int precedence() {
        return 0;
    }

    @Override
    public Predicate not() {
        return new NullityPredicate(expression, !negative);
    }
}
