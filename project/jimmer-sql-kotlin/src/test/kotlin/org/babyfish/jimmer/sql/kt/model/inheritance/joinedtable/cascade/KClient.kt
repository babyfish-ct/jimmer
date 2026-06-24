package org.babyfish.jimmer.sql.kt.model.inheritance.joinedtable.cascade

import org.babyfish.jimmer.sql.*

@Entity
@Table(name = "JOINED_CASCADE_CLIENT")
@Inheritance(
    strategy = InheritanceType.JOINED,
    joinedTableDeleteMode = JoinedTableDeleteMode.DB_CASCADE
)
interface KClient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long

    @Discriminator
    @Column(name = "CLIENT_TYPE")
    val type: String

    val name: String
}
