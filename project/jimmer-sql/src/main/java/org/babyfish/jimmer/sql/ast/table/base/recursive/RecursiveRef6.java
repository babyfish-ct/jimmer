package org.babyfish.jimmer.sql.ast.table.base.recursive;

import org.babyfish.jimmer.sql.ast.*;
import org.babyfish.jimmer.sql.ast.table.RecursiveRef;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.base.BaseTable6;

import java.time.temporal.Temporal;
import java.util.Date;

public interface RecursiveRef6<
        S1 extends Selection<?>,
        S2 extends Selection<?>,
        S3 extends Selection<?>,
        S4 extends Selection<?>,
        S5 extends Selection<?>,
        S6 extends Selection<?>
> extends RecursiveRef<BaseTable6<S1, S2, S3, S4, S5, S6>> {

    <T extends Table<?>> RecursiveRef7<S1, S2, S3, S4, S5, S6, T> table(Class<?> tableType);

    RecursiveRef7<S1, S2, S3, S4, S5, S6, StringExpression> string();

    <N extends Number & Comparable<N>> RecursiveRef7<S1, S2, S3, S4, S5, S6, NumericExpression<N>> numeric(Class<N> numberType);

    <T extends Date> RecursiveRef7<S1, S2, S3, S4, S5, S6, DateExpression<T>> date(Class<T> comparableType);

    <T extends Temporal & Comparable<T>> RecursiveRef7<S1, S2, S3, S4, S5, S6, TemporalExpression<T>> temporal(Class<T> comparableType);

    <T extends Comparable<?>> RecursiveRef7<S1, S2, S3, S4, S5, S6, ComparableExpression<T>> comparable(Class<T> comparableType);

    <T> RecursiveRef7<S1, S2, S3, S4, S5, S6, Expression<T>> value(Class<?> valueType);
}