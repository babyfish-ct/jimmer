package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

import java.util.Collection;
import java.util.List;

public class InCollectionPredicate extends AbstractPredicate {

    private Expression<?> expression;

    private Collection<?> values;

    private boolean negative;

    public InCollectionPredicate(
            Expression<?> expression,
            Collection<?> values,
            boolean negative
    ) {
        this.expression = expression;
        this.values = values;
        this.negative = negative;
    }

    @Override
    public void accept(AstVisitor visitor) {
        ((Ast) expression).accept(visitor);
    }

    @Override
    public void renderTo(SqlBuilder builder) {
        if (values.isEmpty()) {
            builder.sql(negative ? "1 = 1" : "1 = 0");
        } else {
            renderChild((Ast) expression, builder);
            builder.sql(negative ? " not in " : " in ");
            builder.sql("(");
            String separator = "";
            for (Object value : values) {
                builder.sql(separator);
                builder.variable(value);
                separator = ", ";
            }
            builder.sql(")");
        }
    }

    @Override
    public int precedence() {
        return 7;
    }

    @Override
    public Predicate not() {
        return new InCollectionPredicate(expression, values, !negative);
    }
}
