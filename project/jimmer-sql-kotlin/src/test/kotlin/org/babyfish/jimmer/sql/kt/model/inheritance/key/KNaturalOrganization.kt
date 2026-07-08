package org.babyfish.jimmer.sql.kt.model.inheritance.key

import org.babyfish.jimmer.sql.DiscriminatorValue
import org.babyfish.jimmer.sql.Entity

@Entity
@DiscriminatorValue("ORG")
interface KNaturalOrganization : KNaturalClient {

    val taxCode: String
}
