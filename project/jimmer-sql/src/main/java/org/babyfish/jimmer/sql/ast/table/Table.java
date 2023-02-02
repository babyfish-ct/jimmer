package org.babyfish.jimmer.sql.ast.table;

import org.babyfish.jimmer.Static;
import org.babyfish.jimmer.sql.ast.NumericExpression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.fetcher.Fetcher;

public interface Table<E> extends Selection<E>, Props {

    Predicate eq(Table<E> other);

    Predicate isNull();

    Predicate isNotNull();

    NumericExpression<Long> count();

    NumericExpression<Long> count(boolean distinct);

    Selection<E> fetch(Fetcher<E> fetcher);

    <S extends Static<E>> Selection<S> fetch(Class<S> staticType);

    TableEx<E> asTableEx();
}
