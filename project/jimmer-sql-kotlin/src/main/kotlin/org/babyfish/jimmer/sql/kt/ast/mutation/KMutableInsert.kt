package org.babyfish.jimmer.sql.kt.ast.mutation

import org.babyfish.jimmer.kt.DslScope
import org.babyfish.jimmer.sql.kt.ast.expression.KExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KPropExpression
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullBaseTable
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullTableEx

@DslScope
interface KMutableInsert<E : Any, B : KNonNullBaseTable<*>> {

    val table: KNonNullTableEx<E>

    val sourceTable: B

    fun <T : Any> set(target: KPropExpression<T>, source: KExpression<out T>)

    fun onConflictDoNothing()

    fun onConflictDoNothing(vararg targetProps: KPropExpression<*>)
}

interface KMutableInsertReturning<E : Any, B : KNonNullBaseTable<*>> :
    KMutableInsert<E, B>,
    KReturningSelectable
