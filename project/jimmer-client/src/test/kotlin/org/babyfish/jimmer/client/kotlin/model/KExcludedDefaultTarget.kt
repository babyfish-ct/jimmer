package org.babyfish.jimmer.client.kotlin.model

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id

@Entity
interface KExcludedDefaultTarget {

    @Id
    val id: Long
}
