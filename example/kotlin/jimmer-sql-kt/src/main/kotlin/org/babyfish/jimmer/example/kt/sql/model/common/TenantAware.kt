package org.babyfish.jimmer.example.kt.sql.model.common

import org.babyfish.jimmer.sql.MappedSuperclass

@MappedSuperclass
interface TenantAware : BaseEntity {

    val tenant: String
}