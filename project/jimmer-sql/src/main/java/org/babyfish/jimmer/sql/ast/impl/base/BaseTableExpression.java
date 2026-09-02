package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.sql.ast.impl.*;
import org.babyfish.jimmer.sql.ast.impl.query.QueryRenderContext;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;

import java.time.temporal.Temporal;
import java.util.Date;
import java.util.Objects;

class BaseTableExpression<T>
        extends AbstractBaseTableExpression<T, ExpressionImplementor<T>> {

    BaseTableExpression(ExpressionImplementor<T> raw, BaseTableOwner baseTableOwner) {
        super(unwrap(raw), baseTableOwner);
    }

    private static <T> ExpressionImplementor<T> unwrap(ExpressionImplementor<T> raw) {
        if (raw instanceof BaseTableExpression<?>) {
            return ((BaseTableExpression<T>) raw).raw();
        }
        return raw;
    }

    @Override
    protected void renderWithoutReplacement(AbstractSqlBuilder<?> builder) {
        builder.assertSimple().getAstContext().pushStatement(
                getBaseTableOwner().getBaseTable().getQuery().getMutableQuery()
        );
        try {
            QueryRenderContext renderContext = builder.assertSimple().getQueryRenderContext();
            BaseQueryRead read = Objects.requireNonNull(
                    renderContext.getBaseQueryReadSupport().expression(getBaseTableOwner()),
                    "No base-query export selection is available for " + getBaseTableOwner()
            );
            builder
                    .sql(builder.assertSimple().alias(read.getRealBaseTable()))
                    .sql(".c")
                    .sql(Integer.toString(read.index(0)));
        } finally {
            builder.assertSimple().getAstContext().popStatement();
        }
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
