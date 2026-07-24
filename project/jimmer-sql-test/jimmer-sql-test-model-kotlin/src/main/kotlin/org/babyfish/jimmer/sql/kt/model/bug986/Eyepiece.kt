package org.babyfish.jimmer.sql.kt.model.bug986

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.OneToOne

@Entity
interface Eyepiece {

    @Id
    val id: Long

    val name: String

    val eyeRelief: Float

    @OneToOne
    val camera: Camera?
}