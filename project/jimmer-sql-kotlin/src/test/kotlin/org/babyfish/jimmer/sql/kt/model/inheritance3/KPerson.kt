package org.babyfish.jimmer.sql.kt.model.inheritance3

import org.babyfish.jimmer.sql.Entity

@Entity
interface KPerson : KClient {

    val firstName: String

    val lastName: String
}
