package org.babyfish.jimmer.example.kt.graphql.entities.common

import org.babyfish.jimmer.sql.MappedSuperclass

@MappedSuperclass
interface TenantAware : BaseEntity {

    val tenant: String
}