package org.babyfish.jimmer.sql.example.model.common

import org.babyfish.jimmer.sql.MappedSuperclass

@MappedSuperclass
interface TenantAware : BaseEntity {

    val tenant: String
}