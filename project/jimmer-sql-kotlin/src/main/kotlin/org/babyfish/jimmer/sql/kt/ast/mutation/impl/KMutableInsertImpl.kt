package org.babyfish.jimmer.sql.kt.ast.mutation.impl

import org.babyfish.jimmer.sql.ast.Expression
import org.babyfish.jimmer.sql.ast.PropExpression
import org.babyfish.jimmer.sql.ast.impl.mutation.MutableInsertImpl
import org.babyfish.jimmer.sql.kt.ast.expression.KExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KPropExpression
import org.babyfish.jimmer.sql.kt.ast.mutation.KMutableInsertReturning
import org.babyfish.jimmer.sql.kt.ast.mutation.KReturningSelectable
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullBaseTable
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullTableEx
import org.babyfish.jimmer.sql.kt.ast.table.impl.KNonNullTableExImpl

internal class KMutableInsertImpl<E : Any, B : KNonNullBaseTable<*>>(
    private val javaInsert: MutableInsertImpl<*>,
    override val sourceTable: B
) : KMutableInsertReturning<E, B>,
    KReturningSelectable by KReturningSelectableImpl(javaInsert) {

    @Suppress("UNCHECKED_CAST")
    override val table: KNonNullTableEx<E> =
        KNonNullTableExImpl(javaInsert.targetTableImplementor) as KNonNullTableEx<E>

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> set(target: KPropExpression<T>, source: KExpression<out T>) {
        javaInsert.set(target as PropExpression<T>, source as Expression<T>)
    }

    override fun onConflictDoNothing() {
        javaInsert.onConflictDoNothing()
    }

    @Suppress("UNCHECKED_CAST")
    override fun onConflictDoNothing(vararg targetProps: KPropExpression<*>) {
        javaInsert.onConflictDoNothing(
            *targetProps.map { it as PropExpression<*> }.toTypedArray()
        )
    }
}
