package org.babyfish.jimmer.spring.repository

import org.babyfish.jimmer.sql.ast.query.NullOrderMode
import org.babyfish.jimmer.sql.ast.query.OrderMode
import org.babyfish.jimmer.sql.kt.ast.expression.KPropExpression
import org.babyfish.jimmer.sql.kt.ast.expression.asc
import org.babyfish.jimmer.sql.kt.ast.expression.desc
import org.babyfish.jimmer.sql.kt.ast.query.SortDsl
import org.babyfish.jimmer.sql.kt.ast.query.KMutableQuery
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullTable
import org.babyfish.jimmer.sql.kt.ast.table.impl.KTableImplementor
import org.springframework.data.domain.Sort

fun KMutableQuery<*>.orderBy(sort: Sort?) {
    orderBy(*SpringOrders.toOrders((table as KTableImplementor<*>).javaTable, sort))
}

fun <E: Any> KMutableQuery<*>.orderBy(block: (SortDsl<E>.() -> Unit)?) {
    if (block != null) {
        val orders = mutableListOf<SortDsl.Order>()
        block(SortDsl(orders))
        for (order in orders) {
            val expr: KPropExpression<Any> = (table as KNonNullTable<*>).get(order.prop.name)
            val astOrder = if (order.mode == OrderMode.DESC) {
                expr.desc()
            } else {
                expr.asc()
            }
            orderBy(
                when (order.nullOrderMode) {
                    NullOrderMode.NULLS_FIRST -> astOrder.nullsFirst()
                    NullOrderMode.NULLS_LAST -> astOrder.nullsLast()
                    else -> astOrder
                }
            )
        }
    }
}

fun KMutableQuery<*>.orderByIf(condition: Boolean, sort: Sort?) {
    if (condition) {
        orderBy(sort)
    }
}
