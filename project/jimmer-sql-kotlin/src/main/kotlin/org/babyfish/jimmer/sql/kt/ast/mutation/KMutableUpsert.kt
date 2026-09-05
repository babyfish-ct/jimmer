package org.babyfish.jimmer.sql.kt.ast.mutation

import org.babyfish.jimmer.kt.DslScope
import org.babyfish.jimmer.sql.kt.ast.expression.KExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KPropExpression
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullBaseTable
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullTableEx

@DslScope
interface KMutableUpsert<E : Any, B : KNonNullBaseTable<*>> {

    val table: KNonNullTableEx<E>

    val sourceTable: B

    fun <T : Any> key(target: KPropExpression<T>, source: KExpression<out T>)

    fun <T : Any> insert(target: KPropExpression<T>, source: KExpression<out T>)

    /**
     * Assign a physical scalar target only on update, preserving its default on insert.
     * The expression can reference both [table] and [sourceTable].
     */
    fun <T : Any> update(target: KPropExpression<T>, expression: KExpression<out T>)

    fun <T : Any> merge(target: KPropExpression<T>, source: KExpression<out T>)

    fun <T : Any> merge(
        target: KPropExpression<T>,
        insertSource: KExpression<out T>,
        updateExpression: KExpression<out T>
    )

    fun updateWhere(vararg predicates: KNonNullExpression<Boolean>?)
}

interface KMutableUpsertReturning<E : Any, B : KNonNullBaseTable<*>> :
    KMutableUpsert<E, B>,
    KReturningSelectable
