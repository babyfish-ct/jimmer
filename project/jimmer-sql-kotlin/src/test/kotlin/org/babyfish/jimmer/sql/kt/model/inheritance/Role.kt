package org.babyfish.jimmer.sql.kt.model.inheritance

import org.babyfish.jimmer.sql.*

@Entity
interface Role : RoleBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long

    @Transient(RolePermissionCountResolver::class)
    val permissionCount: Int
}