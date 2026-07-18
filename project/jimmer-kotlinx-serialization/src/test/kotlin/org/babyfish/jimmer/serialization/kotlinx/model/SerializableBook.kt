package org.babyfish.jimmer.serialization.kotlinx.model

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id

@Entity
interface SerializableBook {
    @Id
    val id: Long

    val name: String
}
