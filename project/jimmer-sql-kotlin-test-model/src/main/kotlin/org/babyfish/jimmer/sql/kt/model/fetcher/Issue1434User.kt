package org.babyfish.jimmer.sql.kt.model.fetcher

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.JoinTable
import org.babyfish.jimmer.sql.ManyToMany
import org.babyfish.jimmer.sql.Table

@Entity
@Table(name = "ISSUE_1434_USER")
interface Issue1434User {

    @Id
    val id: Long

    val name: String

    @ManyToMany
    @JoinTable(
        name = "ISSUE_1434_USER_DEPARTMENT_MAPPING",
        joinColumnName = "USER_ID",
        inverseJoinColumnName = "DEPARTMENT_ID"
    )
    val departments: List<Issue1434Department>
}
