package org.babyfish.jimmer.sql.kt.model.inheritance

import org.babyfish.jimmer.sql.Key
import org.babyfish.jimmer.sql.MappedSuperclass

@MappedSuperclass
interface NamedEntity {

    @Key
    val name: String
}