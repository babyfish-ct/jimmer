package org.babyfish.jimmer.sql.kt.ast.query

import org.babyfish.jimmer.kt.DslScope
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullPropExpression
import org.babyfish.jimmer.sql.kt.ast.table.KProps

interface AbstractKFilterable<E: Any, P: KProps<E>> : KSubQueryProvider<E> {

    val table: P

    val where: Where

    fun where(vararg predicates: KNonNullExpression<Boolean>?)

    fun where(block: () -> KNonNullPropExpression<Boolean>?)
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
