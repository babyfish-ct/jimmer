package org.babyfish.jimmer.sql.kt.model.inheritance.joinedtable.key

import org.babyfish.jimmer.sql.DiscriminatorValue
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Table

@Entity
@Table(name = "JOINED_KEY_ORGANIZATION")
@DiscriminatorValue("ORG")
interface KKeyOrganization : KKeyClient {

    val taxCode: String
}
