package org.babyfish.jimmer.sql.kt.ast.query

import org.babyfish.jimmer.kt.DslScope
import org.babyfish.jimmer.kt.toImmutableProp
import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.meta.TargetLevel
import org.babyfish.jimmer.sql.ast.Expression
import org.babyfish.jimmer.sql.ast.impl.query.MutableRootQueryImpl
import org.babyfish.jimmer.sql.ast.query.NullOrderMode
import org.babyfish.jimmer.sql.ast.query.OrderMode
import org.babyfish.jimmer.sql.ast.table.Table
import org.babyfish.jimmer.sql.kt.ast.expression.or
import org.babyfish.jimmer.sql.meta.Storage
import kotlin.reflect.KProperty1

@DslScope
class SortDsl<E: Any>(
    private val orders: MutableList<Order> = mutableListOf()
) {

    fun asc(prop: KProperty1<E, *>, nullOrderMode: NullOrderMode = NullOrderMode.UNSPECIFIED) {
        val immutableProp = prop.toImmutableProp()
        if (!immutableProp.isScalar(TargetLevel.OBJECT)) {
            throw IllegalArgumentException("\"$immutableProp\" is not scalar property")
        }
        if (immutableProp.getStorage<Storage>() == null) {
            throw IllegalArgumentException("\"$immutableProp\" is not mapped by database columns")
        }
        orders += Order(immutableProp, OrderMode.ASC, nullOrderMode)
    }

    fun desc(prop: KProperty1<E, *>, nullOrderMode: NullOrderMode = NullOrderMode.UNSPECIFIED) {
        val immutableProp = prop.toImmutableProp()
        if (!immutableProp.isScalar(TargetLevel.OBJECT)) {
            throw IllegalArgumentException("\"$immutableProp\" is not scalar property")
        }
        if (immutableProp.getStorage<Storage>() == null) {
            throw IllegalArgumentException("\"$immutableProp\" is not mapped by database columns")
        }
        orders += Order(immutableProp, OrderMode.DESC, nullOrderMode)
    }

    operator fun plusAssign(orders: List<Order>) {
        this.orders += orders
    }

    internal fun applyTo(query: MutableRootQueryImpl<*>) {
        val table = query.getTable<Table<*>>()
        for (order in orders) {
            val expr = table.get<Expression<*>>(order.prop.name)
            val astOrder = if (order.mode == OrderMode.DESC) {
                expr.desc()
            } else {
                expr.asc()
            }
            query.orderBy(
                when (order.nullOrderMode) {
                    NullOrderMode.NULLS_FIRST -> astOrder.nullsFirst()
                    NullOrderMode.NULLS_LAST -> astOrder.nullsLast()
                    else -> astOrder
                }
            )
        }
    }

    data class Order(
        val prop: ImmutableProp,
        val mode: OrderMode,
        val nullOrderMode: NullOrderMode
    )
}