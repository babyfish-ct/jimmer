package org.babyfish.jimmer.sql.kt.model.mapsid

import org.babyfish.jimmer.sql.JoinColumn
import org.babyfish.jimmer.sql.MappedSuperclass
import org.babyfish.jimmer.sql.MapsId
import org.babyfish.jimmer.sql.OneToOne

@MappedSuperclass
interface KMapsIdMessageDeliveryMessageBase {

    @MapsId
    @OneToOne
    @JoinColumn
    val message: KMapsIdMessage
}
