package org.babyfish.jimmer.sql.kt.model.inheritance

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.EntityInstantiability

@Entity(instantiability = EntityInstantiability.ABSTRACT)
interface KPowerDevice : KBaseNode
