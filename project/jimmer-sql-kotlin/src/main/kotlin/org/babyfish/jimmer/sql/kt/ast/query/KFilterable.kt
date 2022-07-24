package org.babyfish.jimmer.sql.kt.ast.query

import org.babyfish.jimmer.sql.kt.KSubQueries
import org.babyfish.jimmer.sql.kt.KWildSubQueries
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullTable
import kotlin.reflect.KClass

interface KFilterable<E: Any> {

    val table: KNonNullTable<E>

    fun where(vararg predicates: KNonNullExpression<Boolean>)

    fun <X: Any, R> subQuery(
        entityType: KClass<X>,
        block: KMutableSubQuery<X>.() -> KConfigurableTypedSubQuery<R>
    ): KConfigurableTypedSubQuery<R> =
        subQueries.forEntity(entityType, block)

    fun <X: Any> wildSubQuery(
        entityType: KClass<X>,
        block: KMutableSubQuery<X>.() -> Unit
    ): KMutableSubQuery<X> =
        wildSubQueries.forEntity(entityType, block)

    val subQueries: KSubQueries

    val wildSubQueries: KWildSubQueries
}