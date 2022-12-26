package org.babyfish.jimmer.spring.model

import org.babyfish.jimmer.sql.ast.query.NullOrderMode
import org.babyfish.jimmer.sql.ast.query.OrderMode
import org.babyfish.jimmer.sql.kt.ast.query.SortDsl
import org.springframework.data.domain.Sort

fun <E: Any> (SortDsl<E>.() -> Unit).toSort(): Sort {
    val orders = mutableListOf<SortDsl.Order>()
    SortDsl<E>(orders).this()
    val springOrders = orders.map {
        Sort.Order(
            if (it.mode == OrderMode.DESC) Sort.Direction.DESC else Sort.Direction.ASC,
            it.prop.name,
            when (it.nullOrderMode) {
                NullOrderMode.NULLS_FIRST -> Sort.NullHandling.NULLS_FIRST
                NullOrderMode.NULLS_LAST -> Sort.NullHandling.NULLS_LAST
                NullOrderMode.UNSPECIFIED -> Sort.NullHandling.NATIVE
            }
        )
    }
    return Sort.by(springOrders)
}