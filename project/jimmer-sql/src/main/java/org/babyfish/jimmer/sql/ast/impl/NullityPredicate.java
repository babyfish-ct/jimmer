package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.PropExpression;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.render.BatchSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.table.IsNullUtils;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.TableProxies;
import org.babyfish.jimmer.sql.ast.impl.value.PropertyGetter;
import org.babyfish.jimmer.sql.ast.table.spi.PropExpressionImplementor;
import org.babyfish.jimmer.sql.meta.EmbeddedColumns;
import org.babyfish.jimmer.sql.meta.SingleColumn;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

class NullityPredicate extends AbstractPredicate {

    private Expression<?> expression;

    private final boolean negative;

    public NullityPredicate(Expression<?> expression, boolean negative) {
        if (!negative && expression instanceof PropExpression<?>) {
            PropExpressionImplementor<?> propExpr = (PropExpressionImplementor<?>) expression;
            IsNullUtils.isValidIsNullExpression(propExpr);
        }
        this.expression = expression;
        this.negative = negative;
    }

    @Override
    public void accept(@NotNull AstVisitor visitor) {
        ((Ast) expression).accept(visitor);
    }

    @Override
    public void renderTo(@NotNull AbstractSqlBuilder<?> builder) {
        if (expression instanceof PropExpression<?>) {
            PropExpressionImplementor<?> propExpr = (PropExpressionImplementor<?>)expression;
            EmbeddedColumns.Partial partial = propExpr.getPartial(
                    builder.sqlClient().getMetadataStrategy()
            );
            if (partial != null) {
                TableImplementor<?> tableImplementor = TableProxies.resolve(
                        propExpr.getTable(),
                        builder instanceof SqlBuilder ?
                                ((SqlBuilder)builder).getAstContext() :
                                null
                );
                ImmutableProp prop = propExpr.getProp();
                builder.enter(SqlBuilder.ScopeType.AND);
                for (String column : partial) {
                    builder.separator();
                    tableImplementor.renderSelection(
                            prop,
                            propExpr.isRawId(),
                            builder,
                            new SingleColumn(column, false, null, null)
                    );
                    if (negative) {
                        builder.sql(" is not null");
                    } else {
                        builder.sql(" is null");
                    }
                }
                builder.leave();
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
    protected boolean determineHasVirtualPredicate() {
        return hasVirtualPredicate(expression);
    }

    @Override
    protected Ast onResolveVirtualPredicate(AstContext ctx) {
        this.expression = ctx.resolveVirtualPredicate(expression);
        return this;
    }

    @Override
    public int precedence() {
        return 0;
    }

    @Override
    public Predicate not() {
        return new NullityPredicate(expression, !negative);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NullityPredicate)) return false;
        NullityPredicate that = (NullityPredicate) o;
        return negative == that.negative && expression.equals(that.expression);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expression, negative);
    }
}
