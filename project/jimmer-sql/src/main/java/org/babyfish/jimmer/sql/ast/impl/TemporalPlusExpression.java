package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.SqlTimeUnit;
import org.babyfish.jimmer.sql.ast.TemporalExpression;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.jetbrains.annotations.NotNull;

import java.time.temporal.Temporal;

class TemporalPlusExpression<T extends Temporal & Comparable<?>>
        extends AbstractExpression<T>
        implements TemporalExpressionImplementor<T> {

    private TemporalExpression<T> raw;

    private Expression<Long> value;

    private final SqlTimeUnit timeUnit;

    TemporalPlusExpression(TemporalExpression<T> raw, Expression<Long> value, SqlTimeUnit timeUnit) {
        this.raw = raw;
        this.value = value;
        this.timeUnit = timeUnit;
    }

    @Override
    protected boolean determineHasVirtualPredicate() {
        return hasVirtualPredicate(raw) || hasVirtualPredicate(value);
    }

    @Override
    protected Ast onResolveVirtualPredicate(AstContext ctx) {
        raw = ctx.resolveVirtualPredicate(raw);
        value = ctx.resolveVirtualPredicate(value);
        return this;
    }

    @Override
    public void accept(@NotNull AstVisitor visitor) {
        ((Ast) raw).accept(visitor);
        ((Ast) value).accept(visitor);
    }

    @Override
    public void renderTo(@NotNull AbstractSqlBuilder<?> builder) {
        builder.sqlClient().getDialect().renderTimePlus(
                builder,
                precedence(),
                (Ast) raw,
                (Ast) value,
                timeUnit
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<T> getType() {
        return ((ExpressionImplementor<T>) raw).getType();
    }

    @Override
    public int precedence() {
        return 0;
    }
}
