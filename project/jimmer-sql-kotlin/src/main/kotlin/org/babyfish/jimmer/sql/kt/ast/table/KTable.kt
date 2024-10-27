package org.babyfish.jimmer.sql.kt.ast.table

import org.babyfish.jimmer.sql.ast.impl.table.TableSelection
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.expression.isNotNull
import org.babyfish.jimmer.sql.kt.ast.expression.isNull

interface KTable<E: Any> : KProps<E> {

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
    fun asTableEx(): KTableEx<E>
}

fun KTable<*>.isNull(): KNonNullExpression<Boolean> {
    val idProp = (this as TableSelection).immutableType.idProp
    return get<Any>(idProp).isNull()
}

fun KTable<*>.isNotNull(): KNonNullExpression<Boolean> {
    val idProp = (this as TableSelection).immutableType.idProp
    return get<Any>(idProp).isNotNull()
}