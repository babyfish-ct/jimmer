package org.babyfish.jimmer.sql.kt.model.inheritance.enumdiscriminator

import org.babyfish.jimmer.sql.DiscriminatorValue
import org.babyfish.jimmer.sql.Entity

@Entity
@DiscriminatorValue("ORG")
interface KEnumOrganization : KEnumClient {

    val name: String
}
