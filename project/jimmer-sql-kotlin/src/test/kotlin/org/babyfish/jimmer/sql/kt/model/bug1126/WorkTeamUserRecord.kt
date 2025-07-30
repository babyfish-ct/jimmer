package org.babyfish.jimmer.sql.kt.model.bug1126

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.ManyToOne

@Entity
interface WorkTeamUserRecord {
    @Id
    val id: Long

    @ManyToOne
    val team: WorkTeam

    @ManyToOne
    val user: WorkUser
}