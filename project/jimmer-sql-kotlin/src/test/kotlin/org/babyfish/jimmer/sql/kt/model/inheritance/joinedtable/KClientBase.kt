package org.babyfish.jimmer.sql.kt.model.inheritance.joinedtable

import org.babyfish.jimmer.sql.Discriminator
import org.babyfish.jimmer.sql.MappedSuperclass

@MappedSuperclass
interface KClientBase {

    @Discriminator
    val type: String
}
