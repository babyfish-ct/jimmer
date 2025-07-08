package org.babyfish.jimmer.sql.ast.table;

import org.babyfish.jimmer.sql.ast.*;
import org.babyfish.jimmer.sql.ast.table.base.recursive.RecursiveRef1;

import java.time.temporal.Temporal;
import java.util.Date;

public interface RecursiveRef<B extends BaseTable> {

    static <T extends Table<?>> RecursiveRef1<T> table(Class<?> tableType) {
        throw new UnsupportedOperationException();
    }

    static RecursiveRef1<StringExpression> string() {
        throw new UnsupportedOperationException();
    }

    static <N extends Number & Comparable<N>> RecursiveRef1<NumericExpression<N>> numeric(Class<N> numberType) {
        throw new UnsupportedOperationException();
    }

    static <T extends Date> RecursiveRef1<DateExpression<T>> date(Class<T> comparableType) {
        throw new UnsupportedOperationException();
    }

    static <T extends Temporal & Comparable<T>> RecursiveRef1<TemporalExpression<T>> temporal(Class<T> comparableType) {
        throw new UnsupportedOperationException();
    }

    static <T extends Comparable<?>> RecursiveRef1<ComparableExpression<T>> comparable(Class<T> comparableType) {
        throw new UnsupportedOperationException();
    }

    static <T> RecursiveRef1<Expression<T>> value(Class<?> valueType) {
        return null;
    }
}
