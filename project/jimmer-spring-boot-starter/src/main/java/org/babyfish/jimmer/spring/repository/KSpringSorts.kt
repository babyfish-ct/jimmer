package org.babyfish.jimmer.spring.repository

import org.babyfish.jimmer.meta.ImmutableType
import org.babyfish.jimmer.sql.ast.query.NullOrderMode
import org.babyfish.jimmer.sql.ast.query.OrderMode
import org.babyfish.jimmer.sql.kt.ast.expression.KPropExpression
import org.babyfish.jimmer.sql.kt.ast.expression.asc
import org.babyfish.jimmer.sql.kt.ast.expression.desc
import org.babyfish.jimmer.sql.kt.ast.query.SortDsl
import org.babyfish.jimmer.sql.kt.ast.query.KMutableQuery
import org.springframework.data.domain.Sort

fun KMutableQuery<*>.orderBy(sort: Sort?) {
    if (sort != null) {
        for (order in sort) {
            val expr: KPropExpression<Any> = table.get(order.property)
            val astOrder = if (order.isDescending) {
                expr.desc()
            } else {
                expr.asc()
            }
            orderBy(
                when (order.nullHandling) {
                    Sort.NullHandling.NULLS_FIRST -> astOrder.nullsFirst()
                    Sort.NullHandling.NULLS_LAST -> astOrder.nullsLast()
                    else -> astOrder
                }
            )
        }
    }
}

fun <E: Any> KMutableQuery<*>.orderBy(block: (SortDsl<E>.() -> Unit)?) {
    if (block != null) {
        val orders = mutableListOf<SortDsl.Order>()
        block(SortDsl(orders))
        for (order in orders) {
            val expr: KPropExpression<Any> = table.get(order.prop.name)
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

fun <E: Any> Sort?.toSortDslBlock(immutableType: ImmutableType): (SortDsl<E>.() -> Unit)? {
    if (this === null) {
        return null
    }
    val orders = mutableListOf<SortDsl.Order>()
    for (order in this) {
        orders += SortDsl.Order(
            immutableType.getProp(order.property),
            if (order.isDescending) {
                OrderMode.DESC
            } else {
                OrderMode.ASC
            },
            when (order.nullHandling) {
                Sort.NullHandling.NULLS_FIRST -> NullOrderMode.NULLS_FIRST
                Sort.NullHandling.NULLS_LAST -> NullOrderMode.NULLS_LAST
                else -> NullOrderMode.UNSPECIFIED
            }
        )
    }
    if (orders.isEmpty()) {
        return null
    }
    return {
        this += orders
    }
}
