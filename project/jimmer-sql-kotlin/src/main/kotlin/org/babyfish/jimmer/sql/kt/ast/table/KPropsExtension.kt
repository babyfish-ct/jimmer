package org.babyfish.jimmer.sql.kt.ast.table

import org.babyfish.jimmer.sql.ast.query.Order
import org.babyfish.jimmer.sql.kt.ast.table.impl.KTableImplementor

fun KProps<*>.makeOrders(vararg codes: String): List<Order> =
    Order.makeOrders((this as KTableImplementor<*>).javaTable, *codes)
