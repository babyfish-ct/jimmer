package org.babyfish.jimmer.sql.kt.model.bug636

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.GenerationType
import org.babyfish.jimmer.sql.Id

@Entity
interface Server {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long

    val hostName: String

    val isArm: Boolean

    val isSsd: Boolean?
}