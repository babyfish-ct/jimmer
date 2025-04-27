package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.sql.ast.impl.*;
import org.babyfish.jimmer.sql.ast.table.BaseTable;

import java.time.temporal.Temporal;
import java.util.Date;

class BaseTableExpression<T> implements ExpressionImplementor<T> {

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
