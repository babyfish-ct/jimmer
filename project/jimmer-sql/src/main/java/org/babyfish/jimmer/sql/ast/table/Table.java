package org.babyfish.jimmer.sql.ast.table;

import org.babyfish.jimmer.Input;
import org.babyfish.jimmer.sql.ast.NumericExpression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.query.Example;
import org.babyfish.jimmer.sql.fetcher.Fetcher;

public interface Table<E> extends Selection<E>, Props {

    /**
     * Shortcut for `this.id().eq(other.id())`
     */
    Predicate eq(Table<E> other);

    /**
     * QBE
     */
    Predicate eq(Example<E> example);

    /**
     * QBE
     */
    Predicate eq(E example);

    /**
     * QBE
     */
    Predicate eq(Input<E> input);

    Predicate isNull();

    Predicate isNotNull();

    NumericExpression<Long> count();

    NumericExpression<Long> count(boolean distinct);

    Selection<E> fetch(Fetcher<E> fetcher);

    TableEx<E> asTableEx();
}
