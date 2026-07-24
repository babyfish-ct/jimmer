package org.babyfish.jimmer.sql.kt.model.enumeration

import org.babyfish.jimmer.sql.DatabaseValidationIgnore
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.ManyToOne

@DatabaseValidationIgnore
@Entity
interface Article {

    @Id
    val id: Long

    val name: String

    @ManyToOne
    val writer: Writer

    @ManyToOne
    val approver: Approver
}