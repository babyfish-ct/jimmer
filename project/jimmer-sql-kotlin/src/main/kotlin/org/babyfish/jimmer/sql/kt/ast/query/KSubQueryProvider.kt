package org.babyfish.jimmer.sql.kt.ast.query

import org.babyfish.jimmer.sql.kt.KSubQueries
import org.babyfish.jimmer.sql.kt.KWildSubQueries
import kotlin.reflect.KClass

interface KSubQueryProvider<E: Any> {

    fun <X: Any, R, SQ: KConfigurableSubQuery<R>> subQuery(
        entityType: KClass<X>,
        block: KMutableSubQuery<E, X>.() -> SQ
    ): SQ =
        subQueries.forEntity(entityType, block)

    fun <X: Any> wildSubQuery(
        entityType: KClass<X>,
        block: KMutableSubQuery<E, X>.() -> Unit
    ): KMutableSubQuery<E, X> =
        wildSubQueries.forEntity(entityType, block)

    val subQueries: KSubQueries<E>

    val wildSubQueries: KWildSubQueries<E>
}