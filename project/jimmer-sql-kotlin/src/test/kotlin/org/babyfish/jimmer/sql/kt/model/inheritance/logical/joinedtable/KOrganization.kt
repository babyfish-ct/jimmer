package org.babyfish.jimmer.sql.kt.model.inheritance.logical.joinedtable

import org.babyfish.jimmer.sql.DiscriminatorValue
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Table

@Entity
@Table(name = "LOGICAL_JOINED_ORGANIZATION")
@DiscriminatorValue("ORG")
interface KOrganization : KClient {

    val taxCode: String
}
