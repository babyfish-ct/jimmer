package org.babyfish.jimmer.sql.kt.ast.query

import org.babyfish.jimmer.sql.kt.KSubQueries
import org.babyfish.jimmer.sql.kt.KWildSubQueries
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullTable
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullTableEx
import org.babyfish.jimmer.sql.kt.ast.table.KPropsLike
import kotlin.reflect.KClass

interface KSubQueryProvider<P: KPropsLike> {

    fun <X: Any, R, SQ: KConfigurableSubQuery<R>> subQuery(
        entityType: KClass<X>,
        block: KMutableSubQuery<P, KNonNullTableEx<X>>.() -> SQ
    ): SQ =
        subQueries.forEntity(entityType, block)

    fun <X: Any> wildSubQuery(
        entityType: KClass<X>,
        block: KMutableSubQuery<P, KNonNullTableEx<X>>.() -> Unit
    ): KMutableSubQuery<P, KNonNullTableEx<X>> =
        wildSubQueries.forEntity(entityType, block)

    val subQueries: KSubQueries<P>

    val wildSubQueries: KWildSubQueries<P>
}