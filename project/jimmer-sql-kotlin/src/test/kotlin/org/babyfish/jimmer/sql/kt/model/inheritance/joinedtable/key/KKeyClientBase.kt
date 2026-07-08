package org.babyfish.jimmer.sql.kt.model.inheritance.joinedtable.key

import org.babyfish.jimmer.sql.Column
import org.babyfish.jimmer.sql.Discriminator
import org.babyfish.jimmer.sql.Key
import org.babyfish.jimmer.sql.MappedSuperclass

@MappedSuperclass
interface KKeyClientBase {

    @Key
    @Discriminator
    @Column(name = "CLIENT_TYPE")
    val type: String
}
