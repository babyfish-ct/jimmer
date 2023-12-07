package org.babyfish.jimmer.sql.kt.ast.query

import org.babyfish.jimmer.sql.kt.KSubQueries
import org.babyfish.jimmer.sql.kt.KWildSubQueries
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.table.KProps
import kotlin.reflect.KClass

interface AbstractKFilterable<E: Any, P: KProps<E>> {

    val table: P

    val where: Where

    fun where(vararg predicates: KNonNullExpression<Boolean>?)

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

@JvmInline
value class Where(
    private val filterable: AbstractKFilterable<*, *>
) {
    operator fun plusAssign(predicate: KNonNullExpression<Boolean>?) {
        filterable.where(predicate)
    }
}

fun <X> AbstractKFilterable<*, *>.whereIfNotNull(
    value: X?,
    block: (X) -> KNonNullExpression<Boolean>?
) {
    if (value !== null) {
        where(block(value))
    }
}

fun AbstractKFilterable<*, *>. whereIfNotEmpty(
    value: String?,
    block: (String) -> KNonNullExpression<Boolean>?
) {
    value?.takeIf { it.isNotEmpty() }?.let {
        where(block(it))
    }
}

fun AbstractKFilterable<*, *>.whereIfNotBlank(
    value: String?,
    block: (String) -> KNonNullExpression<Boolean>?
) {
    value?.takeIf { it.isNotBlank() }?.let {
        where(block(it))
    }
}
