package org.babyfish.jimmer.client.kotlin.model

import org.babyfish.jimmer.sql.MappedSuperclass
import org.babyfish.jimmer.sql.Serialized

@MappedSuperclass
interface GisBoundary {

    @Serialized
    val points: List<GisPoint>
}