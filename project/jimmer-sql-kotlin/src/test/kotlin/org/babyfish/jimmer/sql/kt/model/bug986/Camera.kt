package org.babyfish.jimmer.sql.kt.model.bug986

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.OneToOne

@Entity
interface Camera {

    @Id
    val id: Long

    val name: String

    @OneToOne(mappedBy = "camera")
    val eyepiece: Eyepiece?

    @OneToOne(mappedBy = "camera")
    val objective: Objective?
}