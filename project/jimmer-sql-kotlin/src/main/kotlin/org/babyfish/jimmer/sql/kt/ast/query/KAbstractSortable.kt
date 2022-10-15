package org.babyfish.jimmer.sql.kt.ast.query

import org.babyfish.jimmer.sql.ast.query.Order
import org.babyfish.jimmer.sql.kt.ast.expression.KExpression
import org.babyfish.jimmer.sql.kt.ast.table.KProps

interface KAbstractSortable<E: Any, P: KProps<E>> : KAbstractFilterable<E, P> {

    fun orderBy(vararg expression: KExpression<*>?)

    fun orderBy(vararg orders: Order?)
}