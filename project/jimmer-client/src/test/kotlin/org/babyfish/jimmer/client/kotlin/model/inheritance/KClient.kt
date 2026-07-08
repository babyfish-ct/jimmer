package org.babyfish.jimmer.client.kotlin.model.inheritance

import org.babyfish.jimmer.sql.Discriminator
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Inheritance
import org.babyfish.jimmer.sql.InheritanceType

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
interface KClient {

    @Id
    val id: Long

    @Discriminator
    val type: KClientType

    val name: String
}
