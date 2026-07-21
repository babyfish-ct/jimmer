package org.babyfish.jimmer.sql.kt.model.fetcher

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Table

@Entity
@Table(name = "ISSUE_1434_DEPARTMENT")
interface Issue1434Department {

    @Id
    val id: Long

    val name: String
}
