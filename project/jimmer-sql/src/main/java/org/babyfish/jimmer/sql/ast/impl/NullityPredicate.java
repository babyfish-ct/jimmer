package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.TableProxies;
import org.babyfish.jimmer.sql.ast.table.spi.PropExpressionImplementor;
import org.babyfish.jimmer.sql.meta.EmbeddedColumns;
import org.babyfish.jimmer.sql.meta.SingleColumn;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.NotNull;

class NullityPredicate extends AbstractPredicate {

    private final Expression<?> expression;

    private final boolean negative;

    public NullityPredicate(Expression<?> expression, boolean negative) {
        this.expression = expression;
        this.negative = negative;
    }

    @Override
    public void accept(@NotNull AstVisitor visitor) {
        ((Ast) expression).accept(visitor);
    }

    @Override
    public void renderTo(@NotNull SqlBuilder builder) {
        if (expression instanceof PropExpressionImplementor<?>) {
            PropExpressionImplementor<?> propExpr = (PropExpressionImplementor<?>)expression;
            EmbeddedColumns.Partial partial = propExpr.getPartial();
            if (partial != null) {
                TableImplementor<?> tableImplementor = TableProxies.resolve(
                        propExpr.getTable(),
                        builder.getAstContext()
                );
                ImmutableProp prop = propExpr.getProp();
                boolean addSeparator = false;
                for (String column : partial) {
                    if (addSeparator) {
                        builder.sql(" and ");
                    } else {
                        addSeparator = true;
                    }
                    tableImplementor.renderSelection(prop, builder, new SingleColumn(column));
                    if (negative) {
                        builder.sql(" is not null");
                    } else {
                        builder.sql(" is null");
                    }
                }
                return;
            }
        }
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
