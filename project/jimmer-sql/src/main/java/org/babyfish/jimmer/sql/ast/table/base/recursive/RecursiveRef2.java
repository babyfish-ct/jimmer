package org.babyfish.jimmer.sql.ast.table.base.recursive;

import org.babyfish.jimmer.sql.ast.*;
import org.babyfish.jimmer.sql.ast.table.RecursiveRef;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.base.BaseTable2;

import java.time.temporal.Temporal;
import java.util.Date;

public interface RecursiveRef2<
        S1 extends Selection<?>,
        S2 extends Selection<?>
> extends RecursiveRef<BaseTable2<S1, S2>> {

    <T extends Table<?>> RecursiveRef3<S1, S2, T> table(Class<?> tableType);

    RecursiveRef3<S1, S2, StringExpression> string();

    <N extends Number & Comparable<N>> RecursiveRef3<S1, S2, NumericExpression<N>> numeric(Class<N> numberType);

    <T extends Date> RecursiveRef3<S1, S2, DateExpression<T>> date(Class<T> comparableType);

    <T extends Temporal & Comparable<T>> RecursiveRef3<S1, S2, TemporalExpression<T>> temporal(Class<T> comparableType);

    <T extends Comparable<?>> RecursiveRef3<S1, S2, ComparableExpression<T>> comparable(Class<T> comparableType);

    <T> RecursiveRef3<S1, S2, Expression<T>> value(Class<?> valueType);
}