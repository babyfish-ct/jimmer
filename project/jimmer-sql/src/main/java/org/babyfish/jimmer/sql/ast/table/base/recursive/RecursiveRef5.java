package org.babyfish.jimmer.sql.ast.table.base.recursive;

import org.babyfish.jimmer.sql.ast.*;
import org.babyfish.jimmer.sql.ast.table.RecursiveRef;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.base.BaseTable5;

import java.time.temporal.Temporal;
import java.util.Date;

public interface RecursiveRef5<
        S1 extends Selection<?>,
        S2 extends Selection<?>,
        S3 extends Selection<?>,
        S4 extends Selection<?>,
        S5 extends Selection<?>
> extends RecursiveRef<BaseTable5<S1, S2, S3, S4, S5>> {

    <T extends Table<?>> RecursiveRef6<S1, S2, S3, S4, S5, T> table(Class<T> tableType);

    RecursiveRef6<S1, S2, S3, S4, S5, StringExpression> string();

    <N extends Number & Comparable<N>> RecursiveRef6<S1, S2, S3, S4, S5, NumericExpression<N>> numeric(Class<N> numberType);

    <T extends Date> RecursiveRef6<S1, S2, S3, S4, S5, DateExpression<T>> date(Class<T> dateType);

    <T extends Temporal & Comparable<T>> RecursiveRef6<S1, S2, S3, S4, S5, TemporalExpression<T>> temporal(Class<T> temporalType);

    <T extends Comparable<?>> RecursiveRef6<S1, S2, S3, S4, S5, ComparableExpression<T>> comparable(Class<T> comparableType);

    <T> RecursiveRef6<S1, S2, S3, S4, S5, Expression<T>> value(Class<?> valueType);
}