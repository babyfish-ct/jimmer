package org.babyfish.jimmer.sql.kt.model.generic

import org.babyfish.jimmer.sql.MappedSuperclass

@MappedSuperclass
interface KBaseNamed {

    val name: String
}
