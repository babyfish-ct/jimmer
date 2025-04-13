package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.SqlTimeUnit;
import org.babyfish.jimmer.sql.ast.TemporalExpression;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.jetbrains.annotations.NotNull;

import java.time.temporal.Temporal;

class TemporalDiffExpression<T extends Temporal & Comparable<?>>
        extends AbstractExpression<Long>
        implements NumericExpressionImplementor<Long> {

    private TemporalExpression<T> raw;

    private TemporalExpression<T> other;

    private final SqlTimeUnit timeUnit;

    TemporalDiffExpression(TemporalExpression<T> raw, TemporalExpression<T> other, SqlTimeUnit timeUnit) {
        this.raw = raw;
        this.other = other;
        this.timeUnit = timeUnit;
    }

    @Override
    protected boolean determineHasVirtualPredicate() {
        return hasVirtualPredicate(raw) || hasVirtualPredicate(other);
    }

    @Override
    protected Ast onResolveVirtualPredicate(AstContext ctx) {
        raw = ctx.resolveVirtualPredicate(raw);
        other = ctx.resolveVirtualPredicate(other);
        return this;
    }

    @Override
    public void accept(@NotNull AstVisitor visitor) {
        ((Ast) raw).accept(visitor);
        ((Ast) other).accept(visitor);
    }

    @Override
    public void renderTo(@NotNull AbstractSqlBuilder<?> builder) {
        builder.sqlClient().getDialect().renderTimeDiff(
                builder,
                precedence(),
                (Ast) raw,
                (Ast) other,
                timeUnit
        );
    }

    @Override
    public Class<Long> getType() {
        return Long.class;
    }

    @Override
    public int precedence() {
        return 0;
    }
}
