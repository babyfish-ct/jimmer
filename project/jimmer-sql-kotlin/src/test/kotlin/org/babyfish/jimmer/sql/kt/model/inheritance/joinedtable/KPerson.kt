package org.babyfish.jimmer.sql.kt.model.inheritance.joinedtable

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Table

@Entity
@Table(name = "JOINED_PERSON")
interface KPerson : KClient {

    val firstName: String

    val lastName: String
}
