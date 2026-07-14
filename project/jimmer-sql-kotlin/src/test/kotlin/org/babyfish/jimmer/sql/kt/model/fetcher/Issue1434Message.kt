package org.babyfish.jimmer.sql.kt.model.fetcher

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.ForeignKeyType
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.JoinColumn
import org.babyfish.jimmer.sql.ManyToOne
import org.babyfish.jimmer.sql.Table
import org.babyfish.jimmer.sql.Transient

@Entity
@Table(name = "ISSUE_1434_MESSAGE")
interface Issue1434Message {

    @Id
    val id: Long

    @ManyToOne
    @JoinColumn(name = "USER_ID", foreignKeyType = ForeignKeyType.FAKE)
    val user: Issue1434User?

    @Transient(Issue1434MessageUserDepartmentNamesResolver::class)
    val userDepartmentNames: String
}
