package org.babyfish.jimmer.sql.kt.model.bug986

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.OneToOne

@Entity
interface Objective {

    @Id
    val id: Long

    val name: String

    val workingDistance: Float

    @OneToOne
    val camera: Camera?
}