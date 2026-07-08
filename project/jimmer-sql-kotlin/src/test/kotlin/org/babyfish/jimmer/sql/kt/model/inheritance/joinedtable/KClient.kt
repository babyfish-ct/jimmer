package org.babyfish.jimmer.sql.kt.model.inheritance.joinedtable

import org.babyfish.jimmer.sql.*

@Entity
@Table(name = "JOINED_CLIENT")
@Inheritance(strategy = InheritanceType.JOINED)
interface KClient : KClientBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long

    val name: String
}
