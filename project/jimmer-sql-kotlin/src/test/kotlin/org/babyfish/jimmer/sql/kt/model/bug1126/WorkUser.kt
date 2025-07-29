package org.babyfish.jimmer.sql.kt.model.bug1126

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.OneToMany

@Entity
interface WorkUser {
    @Id
    val id: Long

    val name: String

    @OneToMany(mappedBy = "user")
    val teamRecords: List<WorkTeamUserRecord>

}