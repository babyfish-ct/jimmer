package org.babyfish.jimmer.sql.kt.model.inheritance.enumdiscriminator

import org.babyfish.jimmer.sql.*

@Entity(instantiability = EntityInstantiability.INSTANTIABLE)
@Table(name = "K_ENUM_CLIENT")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorValue("CLIENT")
interface KEnumClient {

    @Id
    val id: Long

    @Discriminator
    @Column(name = "CLIENT_TYPE")
    val type: KClientType
}
