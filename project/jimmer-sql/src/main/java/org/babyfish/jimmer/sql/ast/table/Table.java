package org.babyfish.jimmer.sql.ast.table;

import org.babyfish.jimmer.View;
import org.babyfish.jimmer.meta.spi.TableDelegate;
import org.babyfish.jimmer.sql.ast.NumericExpression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.table.TableTypeProvider;
import org.babyfish.jimmer.sql.ast.query.Example;
import org.babyfish.jimmer.sql.ast.table.spi.TableLike;
import org.babyfish.jimmer.sql.fetcher.Fetcher;

public interface Table<E> extends TableLike<E>, TableDelegate<E>, TableTypeProvider, Selection<E>, Props {

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

    /**
     * <p>If you must convert table types using the "asTableEx()"
     * function to find corresponding properties in IDE's
     * intelligent suggestions, it likely indicates you are performing
     * table join operations on collection-associated properties, for example:</p>
     *
     * <pre>{@code
     * BookTable table = BookTable.$;
     * sql
     *     .create(table)
     *     // Table join based on collection association `Book.authors`
     *     .where(table.asTableEx().authors().firstName().eq("Alex"))
     *     select(table)
     *     .execute();
     * }</pre>
     *
     * <p>This usage will lead to data duplication, and whether using
     * SQL-level or application-level approaches, you'll need to handle
     * data distinction yourself. More importantly, this duplication
     * will invalidate the pagination mechanism.</p>
     *
     * <p>In fact, this usage is not recommended, and the purpose of
     * "asTableEx()" is to remind you that you're using a feature
     * that might cause problems. In most cases, sub-queries,
     * especially implicit sub-queries, are more recommended. For example:</p>
     *
     * <pre>{@code
     * BookTable table = BookTable.$;
     * sql.createQuery(table)
     *     .where(
     *         table.authors(author ->
     *            author.firstName().eq("Alex")
     *         )
     *     )
     *     .select(table)
     *     .execute();
     * }</pre>
     */
    TableEx<E> asTableEx();
}
