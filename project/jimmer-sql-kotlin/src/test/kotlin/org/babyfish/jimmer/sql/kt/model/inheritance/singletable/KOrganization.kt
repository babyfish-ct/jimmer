package org.babyfish.jimmer.sql.kt.model.inheritance.singletable

import org.babyfish.jimmer.sql.DiscriminatorValue
import org.babyfish.jimmer.sql.Entity

@Entity
@DiscriminatorValue("ORG")
interface KOrganization : KClient {

    val taxCode: String
}
