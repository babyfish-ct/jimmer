package org.babyfish.jimmer.sql.kt.model.inheritance.enumdiscriminator

import org.babyfish.jimmer.sql.DiscriminatorValue
import org.babyfish.jimmer.sql.Entity

@Entity
@DiscriminatorValue("PERSON")
interface KEnumPerson : KEnumClient {

    val firstName: String
}
