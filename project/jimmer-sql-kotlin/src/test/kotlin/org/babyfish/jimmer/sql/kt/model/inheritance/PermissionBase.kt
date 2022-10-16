package org.babyfish.jimmer.sql.kt.model.inheritance

import org.babyfish.jimmer.sql.ManyToOne
import org.babyfish.jimmer.sql.MappedSuperclass

@MappedSuperclass
interface PermissionBase : NamedEntity {

    @ManyToOne
    val role: Role?
}