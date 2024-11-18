package org.babyfish.jimmer.client.kotlin.model

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Serialized

@Entity
interface GisArea : GisBoundary {

    @Id
    val id: Long

    val name: String
}