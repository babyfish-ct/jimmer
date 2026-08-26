package org.babyfish.jimmer.sql.kt.model.inheritance

import org.babyfish.jimmer.sql.Column
import org.babyfish.jimmer.sql.Discriminator
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.EntityInstantiability
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Inheritance
import org.babyfish.jimmer.sql.InheritanceType
import org.babyfish.jimmer.sql.Table

@Entity(instantiability = EntityInstantiability.ABSTRACT)
@Table(name = "BASE_NODE")
@Inheritance(strategy = InheritanceType.JOINED)
interface KBaseNode {

    @Id
    val id: Long

    @Discriminator
    @Column(name = "NODE_TYPE")
    val type: String

    val name: String
}
