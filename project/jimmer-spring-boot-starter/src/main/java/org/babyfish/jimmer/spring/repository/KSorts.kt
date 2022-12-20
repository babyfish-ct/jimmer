package org.babyfish.jimmer.spring.repository

import org.babyfish.jimmer.meta.ImmutableType
import org.babyfish.jimmer.sql.ast.query.OrderMode
import org.babyfish.jimmer.sql.kt.ast.expression.KPropExpression
import org.babyfish.jimmer.sql.kt.ast.expression.asc
import org.babyfish.jimmer.sql.kt.ast.expression.desc
import org.babyfish.jimmer.sql.kt.ast.query.FindDsl
import org.babyfish.jimmer.sql.kt.ast.query.KMutableQuery
import org.springframework.data.domain.Sort

fun KMutableQuery<*>.orderBy(sort: Sort?) {
    if (sort != null) {
        for (order in sort) {
            val expr: KPropExpression<Any> = table.get(order.property)
            orderBy(
                if (order.isDescending) {
                    expr.desc()
                } else {
                    expr.asc()
                }
            )
        }
    }
}

fun <E: Any> KMutableQuery<*>.orderBy(block: (FindDsl<E>.() -> Unit)?) {
    if (block != null) {
        val orders = mutableListOf<FindDsl.Order>()
        block(FindDsl(orders))
        for (order in orders) {
            val expr: KPropExpression<Any> = table.get(order.prop.name)
            orderBy(
                if (order.mode == OrderMode.DESC) {
                    expr.desc()
                } else {
                    expr.asc()
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

fun <E: Any> (FindDsl<E>.() -> Unit).toSort(): Sort {
    val orders = mutableListOf<FindDsl.Order>()
    this(FindDsl(orders))
    return orders.map {
        Sort.Order(
            if (it.mode == OrderMode.DESC) {
                Sort.Direction.DESC
            } else {
                Sort.Direction.ASC
            },
            it.prop.name
        )
    }.let {
        Sort.by(it)
    }
}

fun <E: Any> Sort?.toFindDslBlock(immutableType: ImmutableType): (FindDsl<E>.() -> Unit)? {
    if (this === null) {
        return null
    }
    val orders = mutableListOf<FindDsl.Order>()
    for (order in this) {
        orders += FindDsl.Order(
            immutableType.getProp(order.property),
            if (order.isDescending) {
                OrderMode.DESC
            } else {
                OrderMode.ASC
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
