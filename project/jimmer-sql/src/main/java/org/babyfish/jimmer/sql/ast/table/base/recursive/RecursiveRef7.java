package org.babyfish.jimmer.sql.ast.table.base.recursive;

import org.babyfish.jimmer.sql.ast.*;
import org.babyfish.jimmer.sql.ast.table.RecursiveRef;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.base.BaseTable7;

import java.time.temporal.Temporal;
import java.util.Date;

public interface RecursiveRef7<
        S1 extends Selection<?>,
        S2 extends Selection<?>,
        S3 extends Selection<?>,
        S4 extends Selection<?>,
        S5 extends Selection<?>,
        S6 extends Selection<?>,
        S7 extends Selection<?>
> extends RecursiveRef<BaseTable7<S1, S2, S3, S4, S5, S6, S7>> {

    <T extends Table<?>> RecursiveRef8<S1, S2, S3, S4, S5, S6, S7, T> table(Class<?> tableType);

    RecursiveRef8<S1, S2, S3, S4, S5, S6, S7, StringExpression> string();

    <N extends Number & Comparable<N>> RecursiveRef8<S1, S2, S3, S4, S5, S6, S7, NumericExpression<N>> numeric(Class<N> numberType);

    <T extends Date> RecursiveRef8<S1, S2, S3, S4, S5, S6, S7, DateExpression<T>> date(Class<T> comparableType);

    <T extends Temporal & Comparable<T>> RecursiveRef8<S1, S2, S3, S4, S5, S6, S7, TemporalExpression<T>> temporal(Class<T> comparableType);

    <T extends Comparable<?>> RecursiveRef8<S1, S2, S3, S4, S5, S6, S7, ComparableExpression<T>> comparable(Class<T> comparableType);

    <T> RecursiveRef8<S1, S2, S3, S4, S5, S6, S7, Expression<T>> value(Class<?> valueType);
}
