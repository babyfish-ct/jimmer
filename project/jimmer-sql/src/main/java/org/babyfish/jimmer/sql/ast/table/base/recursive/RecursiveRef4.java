package org.babyfish.jimmer.sql.ast.table.base.recursive;

import org.babyfish.jimmer.sql.ast.*;
import org.babyfish.jimmer.sql.ast.table.RecursiveRef;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.base.BaseTable4;

import java.time.temporal.Temporal;
import java.util.Date;

public interface RecursiveRef4<
        S1 extends Selection<?>,
        S2 extends Selection<?>,
        S3 extends Selection<?>,
        S4 extends Selection<?>
> extends RecursiveRef<BaseTable4<S1, S2, S3, S4>> {

    <T extends Table<?>> RecursiveRef5<S1, S2, S3, S4, T> table(Class<T> tableType);

    RecursiveRef5<S1, S2, S3, S4, StringExpression> string();

    <N extends Number & Comparable<N>> RecursiveRef5<S1, S2, S3, S4, NumericExpression<N>> numeric(Class<N> numberType);

    <T extends Date> RecursiveRef5<S1, S2, S3, S4, DateExpression<T>> date(Class<T> dateType);

    <T extends Temporal & Comparable<T>> RecursiveRef5<S1, S2, S3, S4, TemporalExpression<T>> temporal(Class<T> temporalType);

    <T extends Comparable<?>> RecursiveRef5<S1, S2, S3, S4, ComparableExpression<T>> comparable(Class<T> comparableType);

    <T> RecursiveRef5<S1, S2, S3, S4, Expression<T>> value(Class<?> valueType);
}