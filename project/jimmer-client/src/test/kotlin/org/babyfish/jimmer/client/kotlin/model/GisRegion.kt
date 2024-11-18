package org.babyfish.jimmer.client.kotlin.model

import org.babyfish.jimmer.sql.Embeddable

@Embeddable
interface GisRegion {
    val left: Float
    val top: Float
    val right: Float
    val bottom: Float
}