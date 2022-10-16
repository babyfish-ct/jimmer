package org.babyfish.jimmer.sql.kt.model.inheritance

import org.babyfish.jimmer.sql.DissociateAction
import org.babyfish.jimmer.sql.ManyToOne
import org.babyfish.jimmer.sql.MappedSuperclass
import org.babyfish.jimmer.sql.OnDissociate

@MappedSuperclass
interface AdministratorMetadataBase : NamedEntity {

    val email: String

    val website: String

    @ManyToOne
    @OnDissociate(DissociateAction.DELETE)
    val administrator: Administrator?
}