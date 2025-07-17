package org.babyfish.jimmer.sql.ast.table.base.recursive;

import org.babyfish.jimmer.sql.ast.*;
import org.babyfish.jimmer.sql.ast.table.RecursiveRef;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.base.BaseTable3;

import java.time.temporal.Temporal;
import java.util.Date;

public interface RecursiveRef3<
        S1 extends Selection<?>,
        S2 extends Selection<?>,
        S3 extends Selection<?>
> extends RecursiveRef<BaseTable3<S1, S2, S3>> {

    <T extends Table<?>> RecursiveRef4<S1, S2, S3, T> table(Class<T> tableType);

    RecursiveRef4<S1, S2, S3, StringExpression> string();

    <N extends Number & Comparable<N>> RecursiveRef4<S1, S2, S3, NumericExpression<N>> numeric(Class<N> numberType);

    <T extends Date> RecursiveRef4<S1, S2, S3, DateExpression<T>> date(Class<T> dateType);

    <T extends Temporal & Comparable<T>> RecursiveRef4<S1, S2, S3, TemporalExpression<T>> temporal(Class<T> temporalType);

    <T extends Comparable<?>> RecursiveRef4<S1, S2, S3, ComparableExpression<T>> comparable(Class<T> comparableType);

    <T> RecursiveRef4<S1, S2, S3, Expression<T>> value(Class<?> valueType);
}