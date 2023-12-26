package org.babyfish.jimmer.client.kotlin.model

import org.babyfish.jimmer.sql.Embeddable
import java.math.BigDecimal

/**
 * A location marked by longitude and latitude
 *
 * @property longitude The latitude, from -180 to +180
 */
@Embeddable
interface KCoordinate {

    val longitude: BigDecimal

    /**
     * The latitude, from -90 to +90
     */
    val latitude: BigDecimal
}