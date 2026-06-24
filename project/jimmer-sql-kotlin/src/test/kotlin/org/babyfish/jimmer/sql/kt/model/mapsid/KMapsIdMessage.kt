package org.babyfish.jimmer.sql.kt.model.mapsid

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id

@Entity
interface KMapsIdMessage {

    @Id
    val id: Long

    val text: String
}
