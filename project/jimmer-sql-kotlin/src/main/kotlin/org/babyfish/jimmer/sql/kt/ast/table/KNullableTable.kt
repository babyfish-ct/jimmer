package org.babyfish.jimmer.sql.kt.ast.table

import org.babyfish.jimmer.View
import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.sql.ast.Selection
import org.babyfish.jimmer.sql.fetcher.Fetcher
import org.babyfish.jimmer.sql.kt.ast.expression.KNullablePropExpression
import kotlin.reflect.KClass

interface KNullableTable<E: Any> : KTable<E>, KNullableProps<E>, Selection<E?> {

    fun fetch(fetcher: Fetcher<E>?): Selection<E?>

    fun <S: View<E>> fetch(staticType: KClass<S>): Selection<S?>

    override fun <X : Any> get(prop: String): KNullablePropExpression<X>

    override fun <X : Any> get(prop: ImmutableProp): KNullablePropExpression<X>

    override fun <X : Any> getId(): KNullablePropExpression<X>

    override fun <X : Any> getAssociatedId(prop: String): KNullablePropExpression<X>

    override fun <X : Any> getAssociatedId(prop: ImmutableProp): KNullablePropExpression<X>

    /**
     * If you must convert table types using the "asTableEx()"
     * function to find corresponding properties in IDE's
     * intelligent suggestions, it likely indicates you are performing
     * table join operations on collection-associated properties, for example:
     *
     * ```
     * sql.executeQuery(Book::class) {
     *     // Table join based on collection association `Book.authors`
     *     where(table.asTableEx().authors.firstName eq "Alex")
     *     select(table)
     * }
     * ```
     *
     * This usage will lead to data duplication, and whether using
     * SQL-level or application-level approaches, you'll need to handle
     * data distinction yourself. More importantly, this duplication
     * will invalidate the pagination mechanism.
     *
     * In fact, this usage is not recommended, and the purpose of
     * "asTableEx()" is to remind you that you're using a feature
     * that might cause problems. In most cases, sub-queries,
     * especially implicit sub-queries, are more recommended. For example:
     *
     * ```
     * sql.executeQuery(Book::class) {
     *     where += table.authors {
     *         firstName eq "Alex"
     *     }
     *     select(table)
     * }
     * ```
     */
    override fun asTableEx(): KNullableTableEx<E>
}