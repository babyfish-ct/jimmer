package org.babyfish.jimmer.sql.kt.model.inheritance

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.EntityInstantiability
import org.babyfish.jimmer.sql.Table

@Entity(instantiability = EntityInstantiability.ABSTRACT)
@Table(name = "POWER_DEVICE")
interface KPowerDevice : KBaseNode {

    val voltage: Int
}
