package org.babyfish.jimmer.sql.kt.model.inheritance.key

import org.babyfish.jimmer.sql.Entity

@Entity
interface KNaturalPerson : KNaturalClient {

    val firstName: String

    val lastName: String
}
