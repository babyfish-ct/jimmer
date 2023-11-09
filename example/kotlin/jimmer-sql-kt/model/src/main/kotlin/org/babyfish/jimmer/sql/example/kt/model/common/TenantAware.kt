package org.babyfish.jimmer.sql.example.kt.model.common

import org.babyfish.jimmer.sql.MappedSuperclass

@MappedSuperclass
interface TenantAware {

    val tenant: String
}