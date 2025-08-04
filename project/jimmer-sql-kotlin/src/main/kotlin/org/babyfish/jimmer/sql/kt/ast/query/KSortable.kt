package org.babyfish.jimmer.sql.kt.ast.query

import org.babyfish.jimmer.kt.DslScope
import org.babyfish.jimmer.sql.ast.query.Order
import org.babyfish.jimmer.sql.kt.ast.expression.KExpression
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullTable
import org.babyfish.jimmer.sql.kt.ast.table.KPropsLike

@DslScope
interface KSortable<P: KPropsLike> : KFilterable<P> {

    fun orderBy(vararg expressions: KExpression<*>?)

    fun orderBy(vararg orders: Order?)

    fun orderBy(orders: List<Order?>)
}