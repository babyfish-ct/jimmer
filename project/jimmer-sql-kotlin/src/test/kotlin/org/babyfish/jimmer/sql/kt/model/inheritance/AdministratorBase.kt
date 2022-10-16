package org.babyfish.jimmer.sql.kt.model.inheritance

import org.babyfish.jimmer.sql.JoinTable
import org.babyfish.jimmer.sql.ManyToMany
import org.babyfish.jimmer.sql.MappedSuperclass
import org.babyfish.jimmer.sql.OneToOne

@MappedSuperclass
interface AdministratorBase : NamedEntity {

    @ManyToMany
    @JoinTable(
        name = "ADMINISTRATOR_ROLE_MAPPING",
        joinColumnName = "ADMINISTRATOR_ID",
        inverseJoinColumnName = "ROLE_ID"
    )
    val roles: List<Role>

    @OneToOne(mappedBy = "administrator")
    val metadata: AdministratorMetadata?
}