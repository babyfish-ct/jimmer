package org.babyfish.jimmer.client.kotlin.model

import org.babyfish.jimmer.sql.Embeddable
import java.math.BigDecimal

@Embeddable
interface KCoordinate {
    val longitude: BigDecimal
    val latitude: BigDecimal
}