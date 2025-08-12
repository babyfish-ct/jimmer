package org.babyfish.jimmer.sql.kt.ast.query

import org.babyfish.jimmer.kt.DslScope
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullPropExpression
import org.babyfish.jimmer.sql.kt.ast.table.KPropsLike

@DslScope
interface KFilterable<P: KPropsLike> : KSubQueryProvider<P> {

    val table: P

    val where: Where

    fun where(vararg predicates: KNonNullExpression<Boolean>?)

    fun where(block: () -> KNonNullPropExpression<Boolean>?)
}

@JvmInline
value class Where(
    private val filterable: KFilterable<*>
) {
    operator fun plusAssign(predicate: KNonNullExpression<Boolean>?) {
        filterable.where(predicate)
    }
}

fun <X> KFilterable<*>.whereIfNotNull(
    value: X?,
    block: (X) -> KNonNullExpression<Boolean>?
) {
    if (value !== null) {
        where(block(value))
    }
}

fun KFilterable<*>.whereIfNotEmpty(
    value: String?,
    block: (String) -> KNonNullExpression<Boolean>?
) {
    value?.takeIf { it.isNotEmpty() }?.let {
        where(block(it))
    }
}

fun <T> KFilterable<*>.whereIfNotEmpty(
    value: Collection<T>?,
    block: (Collection<T>) -> KNonNullExpression<Boolean>?
) {
    value?.takeIf { it.isNotEmpty() }?.let {
        where(block(it))
    }
}

fun <T> KFilterable<*>.whereIfNotEmpty(
    value: Array<T>?,
    block: (Array<T>) -> KNonNullExpression<Boolean>?
) {
    value?.takeIf { it.isNotEmpty() }?.let {
        where(block(it))
    }
}

fun KFilterable<*>.whereIfNotBlank(
    value: String?,
    block: (String) -> KNonNullExpression<Boolean>?
) {
    value?.takeIf { it.isNotBlank() }?.let {
        where(block(it))
    }
}
