package org.babyfish.jimmer.sql.kt.model.pg

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import java.net.InetAddress

@Entity
interface Container {

    @Id
    val id: Long

    val address: InetAddress
}