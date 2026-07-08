package org.babyfish.jimmer.client.kotlin.model.inheritance

import org.babyfish.jimmer.sql.DiscriminatorValue
import org.babyfish.jimmer.sql.Entity

@Entity
@DiscriminatorValue("PERSON")
interface KPerson : KClient {

    val firstName: String
}
