package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.sql.ast.impl.*;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.jetbrains.annotations.NotNull;

import java.time.temporal.Temporal;
import java.util.Date;

class BaseTableExpression<T> implements ExpressionImplementor<T>, Ast {

    private final ExpressionImplementor<T> raw;

    private final BaseTableOwner baseTableOwner;

    BaseTableExpression(ExpressionImplementor<T> raw, BaseTableOwner baseTableOwner) {
        if (raw instanceof BaseTableExpression<?>) {
            raw = ((BaseTableExpression<T>)raw).raw;
        }
        this.raw = raw;
        this.baseTableOwner = baseTableOwner;
    }

    BaseTableOwner getBaseTableOwner() {
        return baseTableOwner;
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
        AstContext ctx = visitor.getAstContext();
        ctx.pushStatement((baseTableOwner.baseTable.getQuery()).getMutableQuery());
        ((Ast) this.raw).accept(visitor);
        ctx.popStatement();
    }

    @Override
    public void renderTo(@NotNull AbstractSqlBuilder<?> builder) {
        AstContext ctx = builder.assertSimple().getAstContext();
        ctx.pushStatement((baseTableOwner.baseTable.getQuery()).getMutableQuery());
        BaseSelectionMapper mapper = ctx.getBaseSelectionMapper(baseTableOwner);
        if (mapper != null) {
            builder.sql(mapper.getAlias()).sql(".c").sql(Integer.toString(mapper.expressionIndex()));
        } else { // Recursive

        }
        ctx.popStatement();
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

        Cmp(ExpressionImplementor<T> raw, BaseTableOwner baseTableOwner) {
            super(raw, baseTableOwner);
        }
    }

    static class Str
            extends Cmp<String>
            implements StringExpressionImplementor {

        Str(ExpressionImplementor<String> raw, BaseTableOwner baseTableOwner) {
            super(raw, baseTableOwner);
        }
    }

    static class Num<N extends Number & Comparable<N>>
            extends Cmp<N>
            implements NumericExpressionImplementor<N> {

        Num(ExpressionImplementor<N> raw, BaseTableOwner baseTableOwner) {
            super(raw, baseTableOwner);
        }
    }

    static class Dt<T extends Date> extends Cmp<T> implements DateExpressionImplementor<T> {

        Dt(ExpressionImplementor<T> raw, BaseTableOwner baseTableOwner) {
            super(raw, baseTableOwner);
        }
    }

    static class Tp<T extends Temporal & Comparable<?>> extends Cmp<T> implements TemporalExpressionImplementor<T> {

        Tp(ExpressionImplementor<T> raw, BaseTableOwner baseTableOwner) {
            super(raw, baseTableOwner);
        }
    }
}
