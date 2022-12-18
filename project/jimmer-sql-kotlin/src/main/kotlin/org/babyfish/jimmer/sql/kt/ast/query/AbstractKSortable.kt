package org.babyfish.jimmer.sql.kt.ast.query

import org.babyfish.jimmer.sql.ast.query.Order
import org.babyfish.jimmer.sql.kt.ast.expression.KExpression
import org.babyfish.jimmer.sql.kt.ast.table.KProps

interface AbstractKSortable<E: Any, P: KProps<E>> : AbstractKFilterable<E, P> {

    fun orderBy(vararg expressions: KExpression<*>?)

    fun orderByIf(condition: Boolean, vararg expressions: KExpression<*>?)

    fun orderBy(vararg orders: Order?)

    fun orderByIf(condition: Boolean, vararg orders: Order?)
}