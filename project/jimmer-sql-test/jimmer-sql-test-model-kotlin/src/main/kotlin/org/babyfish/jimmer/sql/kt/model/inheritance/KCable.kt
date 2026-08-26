package org.babyfish.jimmer.sql.kt.model.inheritance

import org.babyfish.jimmer.sql.DiscriminatorValue
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Table

@Entity
@Table(name = "CABLE")
@DiscriminatorValue("CABLE")
interface KCable : KPowerDevice {

    val length: Int
}
