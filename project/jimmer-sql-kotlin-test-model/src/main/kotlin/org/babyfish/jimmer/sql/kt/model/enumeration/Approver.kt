package org.babyfish.jimmer.sql.kt.model.enumeration

import org.babyfish.jimmer.sql.DatabaseValidationIgnore
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id

@DatabaseValidationIgnore
@Entity
interface Approver {

    @Id
    val id: Long

    val name: String

    val gender: Gender?
}