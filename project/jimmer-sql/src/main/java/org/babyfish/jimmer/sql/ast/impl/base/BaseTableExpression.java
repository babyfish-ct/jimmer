package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.sql.ast.impl.*;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.table.BaseTable;
import org.jetbrains.annotations.NotNull;

import java.time.temporal.Temporal;
import java.util.Date;

class BaseTableExpression<T> implements ExpressionImplementor<T>, Ast {

    private final ExpressionImplementor<T> raw;

    private final BaseTable<?> baseTable;

    BaseTableExpression(ExpressionImplementor<T> raw, BaseTable<?> baseTable) {
        this.raw = raw;
        this.baseTable = baseTable;
    }

    @Override
    public Class<T> getType() {
        return raw.getType();
    }

    @Override
    public int precedence() {
        return raw.precedence();
    }

    @Override
    public void accept(@NotNull AstVisitor visitor) {
        ((Ast)this.raw).accept(visitor);
    }

    @Override
    public void renderTo(@NotNull AbstractSqlBuilder<?> builder) {
        ((Ast)this.raw).renderTo(builder);
    }

    @Override
    public boolean hasVirtualPredicate() {
        return ((Ast)this.raw).hasVirtualPredicate();
    }

    @Override
    public Ast resolveVirtualPredicate(AstContext ctx) {
        return ((Ast)this.raw).resolveVirtualPredicate(ctx);
    }

    static class Cmp<T extends Comparable<?>>
            extends BaseTableExpression<T>
            implements ComparableExpressionImplementor<T> {

        Cmp(ExpressionImplementor<T> raw, BaseTable<?> baseTable) {
            super(raw, baseTable);
        }
    }

    static class Str
            extends Cmp<String>
            implements StringExpressionImplementor {

        Str(ExpressionImplementor<String> raw, BaseTable<?> baseTable) {
            super(raw, baseTable);
        }
    }

    static class Num<N extends Number & Comparable<N>>
            extends Cmp<N>
            implements NumericExpressionImplementor<N> {

        Num(ExpressionImplementor<N> raw, BaseTable<?> baseTable) {
            super(raw, baseTable);
        }
    }

    static class Dt<T extends Date> extends Cmp<T> implements DateExpressionImplementor<T> {

        Dt(ExpressionImplementor<T> raw, BaseTable<?> baseTable) {
            super(raw, baseTable);
        }
    }

    static class Tp<T extends Temporal & Comparable<?>> extends Cmp<T> implements TemporalExpressionImplementor<T> {

        Tp(ExpressionImplementor<T> raw, BaseTable<?> baseTable) {
            super(raw, baseTable);
        }
    }
}
