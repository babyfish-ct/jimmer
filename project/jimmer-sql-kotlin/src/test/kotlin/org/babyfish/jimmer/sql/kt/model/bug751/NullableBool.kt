package org.babyfish.jimmer.sql.kt.model.bug751

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id

@Entity
interface NullableBool {

    @Id
    val id: Long

    val value: Boolean?
}