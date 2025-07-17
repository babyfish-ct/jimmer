package org.babyfish.jimmer.sql.ast.table;

import org.babyfish.jimmer.sql.ast.*;
import org.babyfish.jimmer.sql.ast.impl.recursive.AbstractRecursiveRef;
import org.babyfish.jimmer.sql.ast.table.base.recursive.RecursiveRef1;

import java.time.temporal.Temporal;
import java.util.Date;

public interface RecursiveRef<B extends BaseTable> {

    static <T extends Table<?>> RecursiveRef1<T> table(Class<T> tableType) {
        return new AbstractRecursiveRef.Ref1<>(tableType);
    }

    static RecursiveRef1<StringExpression> string() {
        return new AbstractRecursiveRef.Ref1<>(StringExpression.class);
    }

    static <N extends Number & Comparable<N>> RecursiveRef1<NumericExpression<N>> numeric(Class<N> numberType) {
        return new AbstractRecursiveRef.Ref1<>(NumericExpression.class);
    }

    static <T extends Date> RecursiveRef1<DateExpression<T>> date(Class<T> dateType) {
        return new AbstractRecursiveRef.Ref1<>(DateExpression.class);
    }

    static <T extends Temporal & Comparable<T>> RecursiveRef1<TemporalExpression<T>> temporal(Class<T> temporalType) {
        return new AbstractRecursiveRef.Ref1<>(TemporalExpression.class);
    }

    static <T extends Comparable<?>> RecursiveRef1<ComparableExpression<T>> comparable(Class<T> comparableType) {
        return new AbstractRecursiveRef.Ref1<>(ComparableExpression.class);
    }

    static <T> RecursiveRef1<Expression<T>> value(Class<?> valueType) {
        return new AbstractRecursiveRef.Ref1<>(Expression.class);
    }
}
