package org.babyfish.jimmer.sql.kt.model.inheritance

import org.babyfish.jimmer.sql.*

@MappedSuperclass
interface AdministratorMetadataBase : NamedEntity {

    val email: String

    val website: String

    @Key
    @OneToOne(inputNotNull = true)
    @OnDissociate(DissociateAction.DELETE)
    val administrator: Administrator?
}