package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.PropExpression;
import org.babyfish.jimmer.sql.ast.impl.table.JoinUtils;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.TableProxies;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.spi.PropExpressionImplementor;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;
import org.babyfish.jimmer.sql.meta.EmbeddedColumns;
import org.babyfish.jimmer.sql.meta.SingleColumn;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

class NullityPredicate extends AbstractPredicate {

    private Expression<?> expression;

    private final boolean negative;

    public NullityPredicate(Expression<?> expression, boolean negative) {
        if (!negative) {
            if (expression instanceof PropExpression<?>) {
                PropExpressionImplementor<?> propExpr = (PropExpressionImplementor<?>) expression;
                if (!propExpr.getProp().isNullable() && !JoinUtils.hasLeftJoin(propExpr.getTable())) {
                    throw new IllegalArgumentException(
                            "Unable to instantiate `is null` predicate which attempts to check if a " +
                                    "non-null property of root table or inner joined table is null " +
                                    "(eg: `table.parent().isNull()`). " +
                                    "There are two solutions: " +
                                    "1. Use associated id property " +
                                    "(eg: `table.parentId().isNull()`), " +
                                    "2. This non-property must belong to a join table " +
                                    "and table join path needs to have at least one left join " +
                                    "(eg: `table.parent(JoinType.LEFT).isNull()`). " +
                                    "The non-null property is `" + propExpr.getProp().getName() +
                                    "` of table `" + propExpr.getTable().getImmutableType().getClass().getName() + "`."
                    );
                }
            }
        }
        this.expression = expression;
        this.negative = negative;
    }

    @Override
    public void accept(@NotNull AstVisitor visitor) {
        ((Ast) expression).accept(visitor);
    }

    @Override
    public void renderTo(@NotNull SqlBuilder builder) {
        if (expression instanceof PropExpression<?>) {
            PropExpressionImplementor<?> propExpr = (PropExpressionImplementor<?>)expression;
            EmbeddedColumns.Partial partial = propExpr.getPartial(
                    builder.getAstContext().getSqlClient().getMetadataStrategy()
            );
            if (partial != null) {
                TableImplementor<?> tableImplementor = TableProxies.resolve(
                        propExpr.getTable(),
                        builder.getAstContext()
                );
                ImmutableProp prop = propExpr.getProp();
                builder.enter(SqlBuilder.ScopeType.AND);
                for (String column : partial) {
                    builder.separator();
                    tableImplementor.renderSelection(prop, propExpr.isRawId(), builder, new SingleColumn(column, false, null));
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
