package org.babyfish.jimmer.sql.kt.query.tuple

import org.babyfish.jimmer.sql.TypedTuple
import java.math.BigDecimal

@TypedTuple
data class AggregateTuple(
    val storeId: Long,
    val bookCount: Long,
    val minPrice: BigDecimal?,
    val maxPrice: BigDecimal?,
    val avgPrice: BigDecimal?
)