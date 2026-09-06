package org.babyfish.jimmer.sql.kt.ast.mutation

import org.babyfish.jimmer.kt.DslScope
import org.babyfish.jimmer.sql.kt.ast.expression.KExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KPropExpression
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullTableEx
import org.babyfish.jimmer.sql.kt.ast.table.KPropsLike

@DslScope
interface KMutableInsert<E : Any, B : KPropsLike> {

    val table: KNonNullTableEx<E>

    val sourceTable: B

    fun <T : Any> set(target: KPropExpression<T>, source: KExpression<out T>)

    fun onConflictDoNothing()

    fun onConflictDoNothing(vararg targetProps: KPropExpression<*>)
}

interface KMutableInsertReturning<E : Any, B : KPropsLike> :
    KMutableInsert<E, B>,
    KReturningSelectable
