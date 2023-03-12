package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.PropExpression;
import org.babyfish.jimmer.sql.ast.table.spi.PropExpressionImplementor;
import org.babyfish.jimmer.sql.runtime.ExecutionException;
import org.babyfish.jimmer.sql.runtime.ScalarProvider;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

class InCollectionPredicate extends AbstractPredicate {

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
    public void accept(@NotNull AstVisitor visitor) {
        ((Ast) expression).accept(visitor);
    }

    @Override
    public void renderTo(@NotNull SqlBuilder builder) {
        if (values.isEmpty()) {
            builder.sql(negative ? "1 = 1" : "1 = 0");
        } else {
            ScalarProvider<Object, Object> scalarProvider =
                    expression instanceof PropExpression<?> ?
                            builder.getAstContext().getSqlClient().getScalarProvider(
                                    ((PropExpressionImplementor<?>)expression).getProp()
                            ) :
                            null;
            renderChild((Ast) expression, builder);
            builder.sql(negative ? " not in " : " in ");
            builder.sql("(");
            String separator = "";
            for (Object value : values) {
                builder.sql(separator);
                if (value != null && scalarProvider != null) {
                    try {
                        value = scalarProvider.toSql(value);
                    } catch (Exception ex) {
                        throw new ExecutionException(
                                "Cannot convert the value \"" +
                                        value +
                                        "\" of property \"" +
                                        ((PropExpressionImplementor<?>)expression).getProp() +
                                        "\" by \"" +
                                        scalarProvider.getClass().getName() +
                                        "\"",
                                ex
                        );
                    }
                }
                builder.variable(value);
                separator = ", ";
            }
            builder.sql(")");
        }
    }

    @Override
    public int precedence() {
        return 0;
    }

    @Override
    public Predicate not() {
        return new InCollectionPredicate(expression, values, !negative);
    }
}
