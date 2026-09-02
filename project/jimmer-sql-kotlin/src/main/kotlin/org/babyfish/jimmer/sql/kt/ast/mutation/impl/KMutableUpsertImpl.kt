package org.babyfish.jimmer.sql.kt.ast.mutation.impl

import org.babyfish.jimmer.sql.ast.Expression
import org.babyfish.jimmer.sql.ast.Predicate
import org.babyfish.jimmer.sql.ast.PropExpression
import org.babyfish.jimmer.sql.ast.impl.mutation.MutableUpsertImpl
import org.babyfish.jimmer.sql.kt.ast.expression.KExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KPropExpression
import org.babyfish.jimmer.sql.kt.ast.mutation.KMutableUpsertReturning
import org.babyfish.jimmer.sql.kt.ast.mutation.KReturningSelectable
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullBaseTable
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullTableEx
import org.babyfish.jimmer.sql.kt.ast.table.impl.KNonNullTableExImpl

internal class KMutableUpsertImpl<E : Any, B : KNonNullBaseTable<*>>(
    private val javaUpsert: MutableUpsertImpl<*>,
    override val sourceTable: B
) : KMutableUpsertReturning<E, B>,
    KReturningSelectable by KReturningSelectableImpl(javaUpsert) {

    @Suppress("UNCHECKED_CAST")
    override val table: KNonNullTableEx<E> =
        KNonNullTableExImpl(javaUpsert.targetTableImplementor) as KNonNullTableEx<E>

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> key(target: KPropExpression<T>, source: KExpression<out T>) {
        javaUpsert.key(target as PropExpression<T>, source as Expression<T>)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> insert(target: KPropExpression<T>, source: KExpression<out T>) {
        javaUpsert.insert(target as PropExpression<T>, source as Expression<T>)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> merge(target: KPropExpression<T>, source: KExpression<out T>) {
        javaUpsert.merge(target as PropExpression<T>, source as Expression<T>)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> merge(
        target: KPropExpression<T>,
        insertSource: KExpression<out T>,
        updateExpression: KExpression<out T>
    ) {
        javaUpsert.merge(
            target as PropExpression<T>,
            insertSource as Expression<T>,
            updateExpression as Expression<T>
        )
    }

    override fun updateWhere(vararg predicates: KNonNullExpression<Boolean>?) {
        javaUpsert.updateWhere(*predicates.mapNotNull { it as Predicate? }.toTypedArray())
    }
}
