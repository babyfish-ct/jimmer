package org.babyfish.jimmer.spring.repository

import org.babyfish.jimmer.sql.kt.ast.expression.KPropExpression
import org.babyfish.jimmer.sql.kt.ast.expression.asc
import org.babyfish.jimmer.sql.kt.ast.expression.desc
import org.babyfish.jimmer.sql.kt.ast.query.KMutableQuery
import org.babyfish.jimmer.sql.kt.ast.query.KSortable
import org.babyfish.jimmer.sql.kt.ast.table.KProps
import org.springframework.data.domain.Sort

fun KSortable<*>.orderBy(table: KProps<*>, sort: Sort?) {
    if (sort != null) {
        for (order in sort) {
            val expr: KPropExpression<Any> = table.get(order.property)
            val jimmerOrder = if (order.isDescending) {
                expr.desc()
            } else {
                expr.asc()
            }
            orderBy(jimmerOrder)
        }
    }
}

fun KSortable<*>.orderByIf(condition: Boolean, table: KProps<*>, sort: Sort?) {
    if (condition) {
        orderBy(table, sort)
    }
}

fun KMutableQuery<*>.orderBy(sort: Sort?) {
    orderBy(this.table, sort)
}

fun KMutableQuery<*>.orderByIf(condition: Boolean, sort: Sort?) {
    if (condition) {
        orderBy(this.table, sort)
    }
}