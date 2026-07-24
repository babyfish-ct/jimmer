package org.babyfish.jimmer.sql.kt.model.mapsid

import org.babyfish.jimmer.sql.Entity

@Entity
interface KMapsIdMessageDelivery : KMapsIdMessageDeliveryIdBase, KMapsIdMessageDeliveryMessageBase {

    val status: String
}
