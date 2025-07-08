package org.babyfish.jimmer.sql.ast.table.base.recursive;

import org.babyfish.jimmer.sql.ast.*;
import org.babyfish.jimmer.sql.ast.table.RecursiveRef;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.base.BaseTable1;

import java.time.temporal.Temporal;
import java.util.Date;

public interface RecursiveRef1<S1 extends Selection<?>> extends RecursiveRef<BaseTable1<S1>> {

    <T extends Table<?>> RecursiveRef2<S1, T> table(Class<?> tableType);

    RecursiveRef2<S1, StringExpression> string();

    <N extends Number & Comparable<N>> RecursiveRef2<S1, NumericExpression<N>> numeric(Class<N> numberType);

    <T extends Date> RecursiveRef2<S1, DateExpression<T>> date(Class<T> comparableType);

    <T extends Temporal & Comparable<T>> RecursiveRef2<S1, TemporalExpression<T>> temporal(Class<T> comparableType);

    <T extends Comparable<?>> RecursiveRef2<S1, ComparableExpression<T>> comparable(Class<T> comparableType);

    <T> RecursiveRef2<S1, Expression<T>> value(Class<?> valueType);
}
