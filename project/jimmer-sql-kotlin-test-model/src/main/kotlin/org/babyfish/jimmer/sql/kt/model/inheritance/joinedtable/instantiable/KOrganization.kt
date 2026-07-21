package org.babyfish.jimmer.sql.kt.model.inheritance.joinedtable.instantiable

import org.babyfish.jimmer.sql.DiscriminatorValue
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Table

@Entity
@Table(name = "JOINED_INST_ORGANIZATION")
@DiscriminatorValue("ORG")
interface KOrganization : KClient {

    val taxCode: String
}
