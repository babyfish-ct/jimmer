package org.babyfish.jimmer.sql.kt.model.inheritance.joinedtable.instantiable

import org.babyfish.jimmer.sql.*

@Entity(instantiability = EntityInstantiability.INSTANTIABLE)
@Table(name = "JOINED_INST_CLIENT")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorValue("CLIENT")
interface KClient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long

    @Discriminator
    @Column(name = "CLIENT_TYPE")
    val type: String

    val name: String
}
