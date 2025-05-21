package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.sql.ast.impl.*;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.table.BaseTableImplementor;
import org.babyfish.jimmer.sql.ast.table.BaseTable;
import org.jetbrains.annotations.NotNull;

import java.time.temporal.Temporal;
import java.util.Date;

class BaseTableExpression<T> implements ExpressionImplementor<T>, Ast {

    private final ExpressionImplementor<T> raw;

    private final BaseTableOwner baseTableOwner;

    BaseTableExpression(ExpressionImplementor<T> raw, BaseTableOwner baseTableOwner) {
        this.raw = raw;
        this.baseTableOwner = baseTableOwner;
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
        visitor.getAstContext().pushStatement(baseTableOwner.table.getStatement());
        ((Ast)this.raw).accept(visitor);
        visitor.getAstContext().popStatement();
    }

    @Override
    public void renderTo(@NotNull AbstractSqlBuilder<?> builder) {
        AstContext astCtx = builder.assertSimple().assertSimple().getAstContext();
        String sql = astCtx.getBaseColumnMapping().map(raw);
        builder.sql(sql);
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
