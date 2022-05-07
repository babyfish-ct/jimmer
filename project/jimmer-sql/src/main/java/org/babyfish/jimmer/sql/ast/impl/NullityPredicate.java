package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

public class NullityPredicate extends AbstractPredicate {

    private AbstractExpression<?> expression;

    private boolean negative;

    public NullityPredicate(AbstractExpression<?> expression, boolean negative) {
        this.expression = expression;
        this.negative = negative;
    }

    @Override
    public void accept(AstVisitor visitor) {
        expression.accept(visitor);
    }

    @Override
    public void renderTo(SqlBuilder builder) {
        renderChild(expression, builder);
        if (negative) {
            builder.sql(" is not null");
        } else {
            builder.sql(" is null");
        }
    }

    @Override
    protected int precedence() {
        return 0;
    }

    @Override
    public Predicate not() {
        return new NullityPredicate(expression, !negative);
    }
}
