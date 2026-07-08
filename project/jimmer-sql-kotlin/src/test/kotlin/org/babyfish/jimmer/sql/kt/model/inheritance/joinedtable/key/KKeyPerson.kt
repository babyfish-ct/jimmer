package org.babyfish.jimmer.sql.kt.model.inheritance.joinedtable.key

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Table

@Entity
@Table(name = "JOINED_KEY_PERSON")
interface KKeyPerson : KKeyClient {

    val firstName: String

    val lastName: String
}
