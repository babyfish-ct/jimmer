package org.babyfish.jimmer.sql.kt.model.inheritance3

import org.babyfish.jimmer.sql.*

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "CLIENT_TYPE")
interface KClient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long

    val name: String
}
