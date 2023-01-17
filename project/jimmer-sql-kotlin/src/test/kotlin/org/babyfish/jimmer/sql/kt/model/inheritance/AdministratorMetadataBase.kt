package org.babyfish.jimmer.sql.kt.model.inheritance

import org.babyfish.jimmer.pojo.Static
import org.babyfish.jimmer.sql.DissociateAction
import org.babyfish.jimmer.sql.MappedSuperclass
import org.babyfish.jimmer.sql.OnDissociate
import org.babyfish.jimmer.sql.OneToOne

@MappedSuperclass
interface AdministratorMetadataBase : NamedEntity {

    val email: String

    val website: String

    @OneToOne(inputNotNull = true)
    @OnDissociate(DissociateAction.DELETE)
    @Static(alias = "default", name = "administratorId", idOnly = true)
    @Static(alias = "composite")
    val administrator: Administrator?
}