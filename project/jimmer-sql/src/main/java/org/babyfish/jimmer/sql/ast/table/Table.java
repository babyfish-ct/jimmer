package org.babyfish.jimmer.sql.ast.table;

import org.babyfish.jimmer.View;
import org.babyfish.jimmer.meta.spi.TableDelegate;
import org.babyfish.jimmer.sql.ast.NumericExpression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.table.TableTypeProvider;
import org.babyfish.jimmer.sql.ast.query.Example;
import org.babyfish.jimmer.sql.fetcher.Fetcher;

public interface Table<E> extends TableDelegate<E>, TableTypeProvider, Selection<E>, Props {

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
    Predicate eq(View<E> view);

    Predicate isNull();

    Predicate isNotNull();

    NumericExpression<Long> count();

    NumericExpression<Long> count(boolean distinct);

    Selection<E> fetch(Fetcher<E> fetcher);

    <V extends View<E>> Selection<V> fetch(Class<V> viewType);

    TableEx<E> asTableEx();
}
