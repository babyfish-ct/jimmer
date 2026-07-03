package org.babyfish.jimmer.sql.kt.model.inheritance.joinedtable

import org.babyfish.jimmer.sql.*

@Entity
@Table(name = "JOINED_CLIENT_PROJECT")
interface KClientProject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long

    val name: String

    @ManyToOne
    val client: KClient?
}
