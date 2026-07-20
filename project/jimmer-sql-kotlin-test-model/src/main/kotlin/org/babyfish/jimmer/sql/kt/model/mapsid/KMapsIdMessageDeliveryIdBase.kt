package org.babyfish.jimmer.sql.kt.model.mapsid

import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.MappedSuperclass

@MappedSuperclass
interface KMapsIdMessageDeliveryIdBase {

    @Id
    val messageId: Long
}
